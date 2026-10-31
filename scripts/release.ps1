<#
将 Simple 已构建的 Windows 分发复制到同级 SDK，不重新执行构建。
xhwsd@qq.com 2026-10-29
#>
$ErrorActionPreference = 'Stop'
$originalEncoding = [Console]::OutputEncoding
$originalOutputEncoding = $OutputEncoding
$exitCode = 0

try {
	[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
	$OutputEncoding = [Console]::OutputEncoding
	if ($args.Count -ne 0) {
		throw '发布脚本不接受参数；请先运行构建，再直接运行 release.bat 或 release.ps1。'
	}
	$simpleRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
	$sourceRoot = Join-Path $simpleRoot 'dist/windows'
	$targetRoot = [IO.Path]::GetFullPath((Join-Path $simpleRoot '../sdk/simple'))
	$jarNames = @('SimpleCompiler.jar', 'SimpleAndroidRuntime.jar')
	$manifestNames = @('SimpleCompiler.json', 'SimpleAndroidRuntime.json')
	$gradleNames = @('settings.gradle', 'build.gradle', 'gradle.properties')
	$sourceExamples = Join-Path $sourceRoot 'examples'
	$targetExamples = [IO.Path]::GetFullPath((Join-Path $targetRoot 'examples'))

	# 所有必需输入与目标目录先通过检查，缺少交付时不覆盖现有 SDK。
	foreach ($name in $jarNames + $manifestNames + @($gradleNames | ForEach-Object { "gradle/$_" })) {
		$file = Join-Path $sourceRoot $name
		if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
			throw "缺少发布文件：$file"
		}
	}
	$sourceLibraries = Join-Path $sourceRoot 'libs'
	$libraries = @(Get-ChildItem -LiteralPath $sourceLibraries -File -Filter '*.jar')
	if ($libraries.Count -eq 0) {
		throw "缺少编译器依赖，请先执行构建：$sourceLibraries"
	}
	$sourceIcons = Join-Path $sourceRoot 'icons'
	if (-not (Test-Path -LiteralPath $sourceIcons -PathType Container) -or
		@(Get-ChildItem -LiteralPath $sourceIcons -Filter '*.svg' -File).Count -eq 0) {
		throw "缺少交付图标目录：$sourceIcons"
	}
	if (-not (Test-Path -LiteralPath $sourceExamples -PathType Container)) {
		throw "缺少交付示例，请先执行构建：$sourceExamples"
	}
	# 以项目文件发现示例，不要求分类目录具有固定层数；忽略构建目录中的副本。
	$exampleProjects = @(Get-ChildItem -LiteralPath $sourceExamples -Filter 'project.properties' -File -Recurse |
		Where-Object {
			$relative = $_.FullName.Substring($sourceExamples.Length + 1)
			$relative -notmatch '(^|[\\/])build([\\/]|$)'
		} | ForEach-Object { $_.Directory })
	if ($exampleProjects.Count -eq 0) {
		throw "交付示例不包含 Simple 项目：$sourceExamples"
	}
	foreach ($project in $exampleProjects) {
		if (Test-Path -LiteralPath (Join-Path $project.FullName 'build')) {
			throw "分发示例不能携带 build，请重新执行构建：$($project.FullName)"
		}
	}
	if (-not (Test-Path -LiteralPath (Split-Path -Parent $targetRoot) -PathType Container)) {
		throw "SDK 根目录不存在：$(Split-Path -Parent $targetRoot)"
	}
	foreach ($directory in @($targetRoot, (Join-Path $targetRoot 'libs'),
		(Join-Path $targetRoot 'gradle'), (Join-Path $targetRoot 'icons'), $targetExamples)) {
		if (Test-Path -LiteralPath $directory) {
			$item = Get-Item -LiteralPath $directory -Force
			if (-not $item.PSIsContainer -or $item.LinkType) {
				throw "发布目标必须是普通目录：$directory"
			}
		}
	}
	# examples 是受发布管理的目录；复制和清理前拒绝源、目标树中的链接。
	if ([IO.Path]::GetDirectoryName($targetExamples) -ne $targetRoot -or
		[IO.Path]::GetFileName($targetExamples) -ne 'examples') {
		throw "示例交付路径超出 SDK Simple 目录：$targetExamples"
	}
	foreach ($directory in @($sourceExamples, $targetExamples)) {
		if (Test-Path -LiteralPath $directory) {
			$items = @((Get-Item -LiteralPath $directory -Force)) +
				@(Get-ChildItem -LiteralPath $directory -Recurse -Force)
			if (@($items | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }).Count) {
				throw "示例交付目录不能包含路径链接：$directory"
			}
		}
	}

	# SDK 可从空交付目录接收完整产物；清单和图标与本轮二进制一起发布。
	[IO.Directory]::CreateDirectory($targetRoot) | Out-Null
	foreach ($name in $jarNames + $manifestNames) {
		Copy-Item -LiteralPath (Join-Path $sourceRoot $name) -Destination (Join-Path $targetRoot $name) -Force
	}
	# 依赖随同一轮构建分发发布；只清理 libs 第一层已淘汰的 JAR，不影响其它附件。
	$targetLibraries = Join-Path $targetRoot 'libs'
	[IO.Directory]::CreateDirectory($targetLibraries) | Out-Null
	foreach ($file in $libraries) {
		Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $targetLibraries $file.Name) -Force
	}
	foreach ($file in Get-ChildItem -LiteralPath $targetLibraries -File -Filter '*.jar') {
		if (-not (Test-Path -LiteralPath (Join-Path $sourceLibraries $file.Name) -PathType Leaf)) {
			Remove-Item -LiteralPath $file.FullName -Force
		}
	}
	# 图标目录及已有许可附件按分发内容原样复制，不在 SDK 维护另一份源文件。
	Copy-Item -LiteralPath $sourceIcons -Destination $targetRoot -Recurse -Force
	$targetGradle = Join-Path $targetRoot 'gradle'
	[IO.Directory]::CreateDirectory($targetGradle) | Out-Null
	foreach ($name in $gradleNames) {
		Copy-Item -LiteralPath (Join-Path $sourceRoot "gradle/$name") -Destination (Join-Path $targetGradle $name) -Force
	}
	$obsoleteConfiguration = Join-Path $targetGradle 'toolchain.json'
	if (Test-Path -LiteralPath $obsoleteConfiguration -PathType Leaf) {
		Remove-Item -LiteralPath $obsoleteConfiguration -Force
	}
	# 先完整复制，再从深到浅清理旧示例及生成文件；不操作 examples 以外的目录。
	Copy-Item -LiteralPath $sourceExamples -Destination $targetRoot -Recurse -Force
	foreach ($item in Get-ChildItem -LiteralPath $targetExamples -Recurse -Force |
		Sort-Object { $_.FullName.Length } -Descending) {
		if (-not $item.FullName.StartsWith($targetExamples + [IO.Path]::DirectorySeparatorChar,
			[StringComparison]::OrdinalIgnoreCase)) {
			throw "不能清理交付目录以外的文件：$($item.FullName)"
		}
		$relative = $item.FullName.Substring($targetExamples.Length + 1)
		if (-not (Test-Path -LiteralPath (Join-Path $sourceExamples $relative))) {
			if ($item.PSIsContainer) {
				# 子文件已在本轮清理，只删除空目录，不执行递归删除。
				[IO.Directory]::Delete($item.FullName)
			} else {
				Remove-Item -LiteralPath $item.FullName -Force
			}
		}
	}
	Write-Output "发布完成：$targetRoot（示例 $($exampleProjects.Count) 个）"
} catch {
	[Console]::Error.WriteLine($_.Exception.Message)
	$exitCode = 1
} finally {
	[Console]::OutputEncoding = $originalEncoding
	$OutputEncoding = $originalOutputEncoding
}
exit $exitCode
