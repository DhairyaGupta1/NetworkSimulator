
try {
    $src = Join-Path -Path $PSScriptRoot -ChildPath 'Network Simulator\src'
    $out = Join-Path -Path $PSScriptRoot -ChildPath 'out'
    $lib = Join-Path -Path $PSScriptRoot -ChildPath 'Network Simulator\lib'
    New-Item -ItemType Directory -Force -Path $out | Out-Null

    Write-Host "Collecting .java files..."
    $files = Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    if ($files.Count -eq 0) {
        Write-Error "No Java source files found under $src"
        exit 1
    }

    $jars = Get-ChildItem -Path $lib -Filter *.jar | ForEach-Object { $_.FullName }
    $classpath = ($jars -join ';') + ";$out"

    Write-Host "Compiling with JSON library..."
    & javac -cp $classpath -d $out $files
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Compilation failed"
        exit 1
    }

    Write-Host "Running..."
    & java -cp $classpath UI.NetworkEditor
} catch {
    Write-Error $_.Exception.Message
    exit 1
}
