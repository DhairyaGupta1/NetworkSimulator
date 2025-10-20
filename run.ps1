# Compile and run Network Simulator (PowerShell)
# Usage: .\run.ps1  or powershell -ExecutionPolicy Bypass -File .\run.ps1

try {
    $src = Join-Path -Path $PSScriptRoot -ChildPath 'Network Simulator\src'
    $out = Join-Path -Path $PSScriptRoot -ChildPath 'out'
    New-Item -ItemType Directory -Force -Path $out | Out-Null

    Write-Host "Collecting .java files..."
    $files = Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    if ($files.Count -eq 0) {
        Write-Error "No Java source files found under $src"
        exit 1
    }

    Write-Host "Compiling..."
    & javac -d $out $files
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Compilation failed"
        exit 1
    }

    Write-Host "Running..."
    & java -cp $out UI.NetworkEditor
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
