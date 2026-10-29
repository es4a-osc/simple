<#
使用 SDK 的 Gradle 构建 Simple 源码，所有调用参数原样交给 Gradle。
无任务参数时使用 Gradle 工程的默认 build 任务，不强制清理增量结果。
xhwsd@qq.com 2026-10-29
#>
$gradleArguments = @($args)
$ErrorActionPreference = 'Stop'
$originalEncoding = [Console]::OutputEncoding
$originalOutputEncoding = $OutputEncoding
$originalEnvironment = @{}
foreach ($name in @('JAVA_HOME', 'ANDROID_HOME', 'SIMPLE_HOME')) {
	$originalEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$exitCode = 0

try {
	[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
	$OutputEncoding = [Console]::OutputEncoding
	$simpleRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
	$sdkRoot = [IO.Path]::GetFullPath((Join-Path $simpleRoot '../sdk'))
	$javaHome = Join-Path $sdkRoot 'tools/jdk-11.0.32.1'
	$gradleHome = Join-Path $sdkRoot 'tools/gradle-8.14.6'
	$javaFile = Join-Path $javaHome 'bin/java.exe'
	foreach ($file in @($javaFile, (Join-Path $gradleHome 'lib/gradle-launcher-8.14.6.jar'),
		(Join-Path $simpleRoot 'gradle/settings.gradle'), (Join-Path $simpleRoot 'gradle/build.gradle'))) {
		if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
			throw "缺少构建文件：$file"
		}
	}

	# 只在本次脚本调用中提供固定 SDK 环境，退出时恢复调用方的原值。
	$env:JAVA_HOME = $javaHome
	$env:ANDROID_HOME = Join-Path $sdkRoot 'tools/android-sdk'
	$env:SIMPLE_HOME = Join-Path $simpleRoot 'dist/windows'
	$arguments = @('-Dfile.encoding=UTF-8', '-Dsun.stdout.encoding=UTF-8', '-Dsun.stderr.encoding=UTF-8',
		'-cp', (Join-Path $gradleHome 'lib/*'), 'org.gradle.launcher.GradleMain',
		'--no-daemon', '--console=plain', '-p', (Join-Path $simpleRoot 'gradle'),
		'--project-cache-dir', (Join-Path $simpleRoot 'build/cache')) + $gradleArguments
	# Windows PowerShell 5.1 会拆分含空格的等号参数；按 Windows 引号规则构造原生命令行。
	$quotedArguments = foreach ($argument in $arguments) {
		$quoted = [regex]::Replace([string]$argument, '(\\*)"', '$1$1\"')
		$quoted = [regex]::Replace($quoted, '(\\+)$', '$1$1')
		'"' + $quoted + '"'
	}
	$start = [Diagnostics.ProcessStartInfo]::new()
	$start.FileName = $javaFile
	$start.Arguments = $quotedArguments -join ' '
	$start.UseShellExecute = $false
	$start.CreateNoWindow = $true
	$start.RedirectStandardOutput = $true
	$start.RedirectStandardError = $true
	$start.StandardOutputEncoding = [Text.UTF8Encoding]::new($false)
	$start.StandardErrorEncoding = [Text.UTF8Encoding]::new($false)
	$process = [Diagnostics.Process]::Start($start)
	try {
		# 两路输出同时异步读取，在当前线程逐行转发，避免错误管道堵塞或延迟到构建结束。
		$streams = @(
			@{ reader = $process.StandardOutput; writer = [Console]::Out; pending = $process.StandardOutput.ReadLineAsync() },
			@{ reader = $process.StandardError; writer = [Console]::Error; pending = $process.StandardError.ReadLineAsync() }
		)
		while ($streams.Count -gt 0) {
			[Threading.Tasks.Task]::WaitAny([Threading.Tasks.Task[]]@($streams | ForEach-Object { $_.pending })) | Out-Null
			$remaining = @()
			foreach ($stream in $streams) {
				if ($stream.pending.IsCompleted) {
					$line = $stream.pending.GetAwaiter().GetResult()
					if ($null -eq $line) { continue }
					$stream.writer.WriteLine($line)
					$stream.pending = $stream.reader.ReadLineAsync()
				}
				$remaining += $stream
			}
			$streams = $remaining
		}
		$process.WaitForExit()
		$exitCode = $process.ExitCode
	} finally {
		$process.Dispose()
	}
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
exit $exitCode
