<#
使用本次源码分发编译运行库设备测试，安装后启动应用，不执行源码构建或发布。
默认结束时等待按键；自动化调用使用 -NoPause，仍接受原 --no-pause 参数。
xhwsd@qq.com 2026-10-30
#>
param ([switch]$NoPause)

$ErrorActionPreference = 'Stop'
$originalEncoding = [Console]::OutputEncoding
$originalOutputEncoding = $OutputEncoding
$originalEnvironment = @{}
foreach ($name in @('JAVA_HOME', 'ANDROID_HOME', 'SIMPLE_HOME', 'LIBRARIES_HOME', 'ES4A_SDK_HOME')) {
	$originalEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$exitCode = 0

try {
	if ($args.Count -eq 1 -and $args[0] -eq '--no-pause') {
		$NoPause = $true
	} elseif ($args.Count -ne 0) {
		throw '设备测试脚本只接受 -NoPause 或 --no-pause。'
	}
	[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
	$OutputEncoding = [Console]::OutputEncoding
	$simpleRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
	$sdkRoot = [IO.Path]::GetFullPath((Join-Path $simpleRoot '../sdk'))
	$project = Join-Path $simpleRoot 'tests/simple/runtime/DeviceTests/project.properties'
	$env:JAVA_HOME = Join-Path $sdkRoot 'tools/jdk-11.0.32.1'
	$env:ANDROID_HOME = Join-Path $sdkRoot 'tools/android-sdk'
	$env:SIMPLE_HOME = Join-Path $simpleRoot 'dist/windows'
	$env:LIBRARIES_HOME = Join-Path $sdkRoot 'libraries'
	$env:ES4A_SDK_HOME = $sdkRoot
	$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
	$compiler = Join-Path $env:SIMPLE_HOME 'SimpleCompiler.jar'
	$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
	$platform = Join-Path $env:ANDROID_HOME 'platforms/android-30'
	$aapt = Join-Path $platform 'tools/aapt2_utf8.exe'
	if (-not (Test-Path -LiteralPath $aapt -PathType Leaf)) {
		$aapt = Join-Path $platform 'tools/aapt2.exe'
	}
	foreach ($file in @($project, $java, $compiler, $adb, $aapt)) {
		if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
			throw "未找到文件：$file"
		}
	}
	$classPath = @((Join-Path $platform 'tools/lib/d8.jar'), $compiler, (Join-Path $simpleRoot 'libs/*'))
	$compilerArguments = @('-Dfile.encoding=UTF-8', '-Dsun.stdout.encoding=UTF-8', '-Dsun.stderr.encoding=UTF-8',
		'-classpath', ($classPath -join [IO.Path]::PathSeparator), 'simple.compiler.Main')

	# 测试源码使用本轮 dist 编译器；构建目录也由同一编译器读取，不写死 APK 路径。
	$buildDirectory = @(& $java @compilerArguments '--build-directory' $project)
	if ($LASTEXITCODE -ne 0 -or $buildDirectory.Count -ne 1 -or
		[string]::IsNullOrWhiteSpace([string]$buildDirectory[0]) -or
		-not [IO.Path]::IsPathRooted([string]$buildDirectory[0])) {
		throw '无法取得设备测试项目的构建目录。'
	}
	$apk = Join-Path ([string]$buildDirectory[0]) 'apks/signed.apk'
	Write-Host '[编译应用]'
	& $java @compilerArguments $project | ForEach-Object { Write-Host $_ }
	if ($LASTEXITCODE -ne 0) {
		throw "编译应用失败，编译器退出码：$LASTEXITCODE"
	}
	if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) {
		throw "未找到应用文件：$apk"
	}

	# ANDROID_SERIAL 沿用 ADB 的设备选择约定，不改写调用方的设备配置。
	$deviceArguments = @()
	if ($env:ANDROID_SERIAL) {
		$deviceArguments = @('-s', $env:ANDROID_SERIAL)
	}
	Write-Host
	Write-Host '[安装应用]'
	& $adb @deviceArguments 'install' '-r' $apk
	if ($LASTEXITCODE -ne 0) {
		throw "应用安装失败，ADB 退出码：$LASTEXITCODE"
	}
	Write-Host '安装应用成功。'

	# 包名来自本次 APK，只有实际启动成功才报告完成。
	$badging = @(& $aapt 'dump' 'badging' $apk)
	if ($LASTEXITCODE -ne 0) {
		throw "读取 APK 包名失败，AAPT2 退出码：$LASTEXITCODE"
	}
	$package = $null
	foreach ($line in $badging) {
		if ($line -match "^package: name='([^']+)'") {
			$package = $Matches[1]
			break
		}
	}
	if (-not $package) {
		throw "未能从 APK 取得应用包名：$apk"
	}
	Write-Host
	Write-Host '[启动应用]'
	$launch = @(& $adb @deviceArguments 'shell' 'am' 'start' '-W' '-n' "$package/simple.runtime.android.MainActivity")
	$launch | ForEach-Object { Write-Host $_ }
	if ($LASTEXITCODE -ne 0 -or -not ($launch -match '^Status:\s+ok\s*$')) {
		throw '应用启动失败，请检查 ADB 输出。'
	}
	Write-Host '启动应用成功。'
} catch {
	[Console]::Error.WriteLine($_.Exception.Message)
	$exitCode = 1
} finally {
	foreach ($name in $originalEnvironment.Keys) {
		[Environment]::SetEnvironmentVariable($name, $originalEnvironment[$name], 'Process')
	}
	[Console]::OutputEncoding = $originalEncoding
	$OutputEncoding = $originalOutputEncoding
}

if (-not $NoPause -and -not [Console]::IsInputRedirected) {
	Write-Host
	Write-Host '按任意键继续...' -NoNewline
	[Console]::ReadKey($true) | Out-Null
}
exit $exitCode
