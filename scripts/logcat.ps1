<#
读取运行库设备测试应用的日志，按完整 UID 筛选，应用重启后继续有效。
参数原样交给 logcat，例如 -d -t 200；ANDROID_SERIAL 可指定设备。
xhwsd@qq.com 2026-10-30
#>
$logcatArguments = @($args)
$ErrorActionPreference = 'Stop'
$originalEncoding = [Console]::OutputEncoding
$originalOutputEncoding = $OutputEncoding
$exitCode = 0

function Start-AdbProcess {
	<#
	使用 UTF-8 管道启动 ADB；统一转义参数，不经由命令解释器拼接执行。
	#>
	param ([string[]]$Arguments)
	$quotedArguments = foreach ($argument in $Arguments) {
		$quoted = [regex]::Replace([string]$argument, '(\\*)"', '$1$1\"')
		$quoted = [regex]::Replace($quoted, '(\\+)$', '$1$1')
		'"' + $quoted + '"'
	}
	$start = [Diagnostics.ProcessStartInfo]::new()
	$start.FileName = $adb
	$start.Arguments = $quotedArguments -join ' '
	$start.UseShellExecute = $false
	$start.CreateNoWindow = $true
	$start.RedirectStandardOutput = $true
	$start.RedirectStandardError = $true
	$start.StandardOutputEncoding = [Text.UTF8Encoding]::new($false)
	$start.StandardErrorEncoding = [Text.UTF8Encoding]::new($false)
	return [Diagnostics.Process]::Start($start)
}

function Invoke-AdbQuery {
	<#
	并行读取短查询的两路输出，保留退出码，供兼容分支判断实际支持情况。
	#>
	param ([string[]]$Arguments)
	$process = Start-AdbProcess $Arguments
	try {
		$output = $process.StandardOutput.ReadToEndAsync()
		$diagnostic = $process.StandardError.ReadToEndAsync()
		$process.WaitForExit()
		return @{
			code = $process.ExitCode
			output = $output.GetAwaiter().GetResult()
			diagnostic = $diagnostic.GetAwaiter().GetResult()
		}
	} finally {
		$process.Dispose()
	}
}

try {
	[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
	$OutputEncoding = [Console]::OutputEncoding
	$sdkRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../sdk'))
	$adb = Join-Path $sdkRoot 'tools/android-sdk/platform-tools/adb.exe'
	if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) {
		throw "未找到 ADB 工具：$adb"
	}
	$package = 'simple.runtime.tests'
	$serial = $env:ANDROID_SERIAL
	$devices = Invoke-AdbQuery @('devices')
	if ($devices.code -ne 0) {
		throw "读取设备列表失败：$($devices.diagnostic.Trim())"
	}
	$online = @()
	foreach ($line in $devices.output -split '\r?\n') {
		if ($line -match '^(\S+)\s+device\s*$') {
			$online += $Matches[1]
		}
	}
	if (-not $serial) {
		# 保持原脚本的默认选择：最后一个在线模拟器，不自动选中 USB 真机。
		foreach ($candidate in $online) {
			if ($candidate -match '^(emulator-[^\s]+|127\.0\.0\.1:\d+|localhost:\d+)$') {
				$serial = $candidate
			}
		}
	}
	if (-not $serial) {
		throw '未找到在线模拟器，请先启动模拟器，或通过 ANDROID_SERIAL 指定设备。'
	}
	if ($serial -notin $online) {
		throw "指定设备不在线或尚未授权：$serial"
	}

	# 新系统优先查询当前用户的完整 UID；旧系统继续从 dumpsys 的 userId 读取。
	$query = Invoke-AdbQuery @('-s', $serial, 'shell', 'pm', 'list', 'packages', '-U', '--user', 'current', $package)
	$appUid = $null
	if ($query.code -eq 0 -and $query.output -match ('(?m)^package:' + [regex]::Escape($package) + '\s+uid:(\d+)\s*$')) {
		$appUid = $Matches[1]
	}
	if (-not $appUid) {
		$query = Invoke-AdbQuery @('-s', $serial, 'shell', 'dumpsys', 'package', $package)
		if ($query.code -eq 0 -and $query.output -match '(?m)^\s*userId=(\d+)\s*$') {
			$appUid = $Matches[1]
		}
	}
	if (-not $appUid) {
		throw "未能取得应用 UID，请确认设备 $serial 已安装：$package"
	}
	$help = Invoke-AdbQuery @('-s', $serial, 'shell', 'logcat', '--help')
	$nativeFilter = $help.code -eq 0 -and ($help.output + $help.diagnostic).Contains('--uid=')
	if ($nativeFilter) {
		$arguments = @('-s', $serial, 'logcat', "--uid=$appUid", '-v', 'threadtime') + $logcatArguments
	} else {
		# Android 7 不支持 --uid；固定 UID 列格式后在本机筛选，不截断长行。
		$arguments = @('-s', $serial, 'logcat') + $logcatArguments + @('-v', 'threadtime', '-v', 'uid')
	}
	$pattern = '^\d{2}-\d{2} \S+\s+' + [regex]::Escape($appUid) + '\s'
	$process = Start-AdbProcess $arguments
	try {
		# 实时转发两路输出，避免错误管道堵塞；无匹配日志仍返回 ADB 自身的退出状态。
		$streams = @(
			@{ reader = $process.StandardOutput; writer = [Console]::Out; filter = -not $nativeFilter; pending = $process.StandardOutput.ReadLineAsync() },
			@{ reader = $process.StandardError; writer = [Console]::Error; filter = $false; pending = $process.StandardError.ReadLineAsync() }
		)
		while ($streams.Count -gt 0) {
			[Threading.Tasks.Task]::WaitAny([Threading.Tasks.Task[]]@($streams | ForEach-Object { $_.pending })) | Out-Null
			$remaining = @()
			foreach ($stream in $streams) {
				if ($stream.pending.IsCompleted) {
					$line = $stream.pending.GetAwaiter().GetResult()
					if ($null -eq $line) {
						continue
					}
					if (-not $stream.filter -or $line -match $pattern) {
						$stream.writer.WriteLine($line)
					}
					$stream.pending = $stream.reader.ReadLineAsync()
				}
				$remaining += $stream
			}
			$streams = $remaining
		}
		$process.WaitForExit()
		$exitCode = $process.ExitCode
	} finally {
		if (-not $process.HasExited) {
			$process.Kill()
		}
		$process.Dispose()
	}
} catch {
	[Console]::Error.WriteLine($_.Exception.Message)
	$exitCode = 1
} finally {
	[Console]::OutputEncoding = $originalEncoding
	$OutputEncoding = $originalOutputEncoding
}
exit $exitCode
