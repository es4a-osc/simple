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

	# 所有必需输入与目标目录先通过检查，缺少交付时不覆盖现有 SDK。
	foreach ($name in $jarNames + $manifestNames + @($gradleNames | ForEach-Object { "gradle/$_" })) {
		$file = Join-Path $sourceRoot $name
		if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
			throw "缺少发布文件：$file"
		}
	}
	$sourceLibraries = Join-Path $sourceRoot 'libs'
	$libraries = @(Get-ChildItem -LiteralPath $sourceLibraries -File)
	if (@($libraries | Where-Object { $_.Extension -eq '.jar' }).Count -eq 0) {
		throw "缺少编译器依赖 JAR：$sourceLibraries"
	}
	$sourceIcons = Join-Path $sourceRoot 'icons'
	if (-not (Test-Path -LiteralPath $sourceIcons -PathType Container) -or
		@(Get-ChildItem -LiteralPath $sourceIcons -Filter '*.svg' -File).Count -eq 0) {
		throw "缺少交付图标目录：$sourceIcons"
	}
	if (-not (Test-Path -LiteralPath (Split-Path -Parent $targetRoot) -PathType Container)) {
		throw "SDK 根目录不存在：$(Split-Path -Parent $targetRoot)"
	}
	foreach ($directory in @($targetRoot, (Join-Path $targetRoot 'libs'),
		(Join-Path $targetRoot 'gradle'), (Join-Path $targetRoot 'icons'))) {
		if (Test-Path -LiteralPath $directory) {
			$item = Get-Item -LiteralPath $directory -Force
			if (-not $item.PSIsContainer -or $item.LinkType) {
				throw "发布目标必须是普通目录：$directory"
			}
		}
	}

	# SDK 可从空交付目录接收完整产物；清单和图标与本轮二进制一起发布。
	[IO.Directory]::CreateDirectory($targetRoot) | Out-Null
	foreach ($name in $jarNames + $manifestNames) {
		Copy-Item -LiteralPath (Join-Path $sourceRoot $name) -Destination (Join-Path $targetRoot $name) -Force
	}
	$targetLibraries = Join-Path $targetRoot 'libs'
	[IO.Directory]::CreateDirectory($targetLibraries) | Out-Null
	foreach ($file in $libraries) {
		Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $targetLibraries $file.Name) -Force
	}

	# 只清理目标 libs 第一层已不再交付的 JAR，保留其它已有文件。
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
	Write-Output "发布完成：$targetRoot"
} catch {
	[Console]::Error.WriteLine($_.Exception.Message)
	$exitCode = 1
} finally {
	[Console]::OutputEncoding = $originalEncoding
	$OutputEncoding = $originalOutputEncoding
}
exit $exitCode
