[CmdletBinding()]
param(
    [string]$JavaHome = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot',
    [string]$DbUrl = 'jdbc:postgresql://127.0.0.1:5432/minerva',
    [string]$DbUsername = 'postgres'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$mavenWrapper = Join-Path $repoRoot 'minerva\mvnw.cmd'
$pomFile = Join-Path $repoRoot 'minerva\pom.xml'

if (-not (Test-Path -LiteralPath $JavaHome -PathType Container)) {
    throw "No se encontró JAVA_HOME en '$JavaHome'. Indicá otro JDK con -JavaHome <ruta>."
}

$javaExecutable = Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable -PathType Leaf)) {
    throw "No se encontró java.exe en '$javaExecutable'. Indicá un JDK válido con -JavaHome <ruta>."
}

$javaVersionOutput = @(& $javaExecutable --version 2>&1)
if ($LASTEXITCODE -ne 0 -or $javaVersionOutput.Count -eq 0) {
    throw "No se pudo consultar la versión de Java en '$javaExecutable'."
}

$javaVersionLine = [string]$javaVersionOutput[0]
if ($javaVersionLine -notmatch '(?i)^(?:openjdk|java)\s+25(?:[\.\s]|$)') {
    throw "Minerva requiere Java 25, pero '$javaExecutable --version' informó: $javaVersionLine"
}

$tcpClient = [System.Net.Sockets.TcpClient]::new()
try {
    $connection = $tcpClient.ConnectAsync('127.0.0.1', 5432)
    if (-not $connection.Wait([TimeSpan]::FromSeconds(3)) -or -not $tcpClient.Connected) {
        throw 'timeout'
    }
}
catch {
    [Console]::Error.WriteLine('PostgreSQL no está disponible en 127.0.0.1:5432. Iniciá o reiniciá PostgreSQL y volvé a ejecutar este script. Maven no fue ejecutado.')
    exit 1
}
finally {
    $tcpClient.Dispose()
}

if (-not (Test-Path -LiteralPath $mavenWrapper -PathType Leaf) -or
    -not (Test-Path -LiteralPath $pomFile -PathType Leaf)) {
    throw "No se encontraron el Maven Wrapper o el pom.xml esperados debajo de '$repoRoot\minerva'."
}

$securePassword = Read-Host 'Contraseña de PostgreSQL' -AsSecureString
$passwordPointer = [IntPtr]::Zero
$plainPassword = $null

$managedEnvironment = @(
    'JAVA_HOME',
    'DB_URL',
    'DB_USERNAME',
    'DB_PASSWORD',
    'JWT_SECRET',
    'SPRING_PROFILES_ACTIVE',
    'MINERVA_DEMO_CATALOG_ENABLED'
)
$previousEnvironment = @{}
foreach ($name in $managedEnvironment) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)

    $env:JAVA_HOME = $JavaHome
    $env:DB_URL = $DbUrl
    $env:DB_USERNAME = $DbUsername
    $env:DB_PASSWORD = $plainPassword
    if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
        $randomBytes = [byte[]]::new(32)
        [Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
        $env:JWT_SECRET = [Convert]::ToBase64String($randomBytes)
        [Array]::Clear($randomBytes, 0, $randomBytes.Length)
    }
    $env:SPRING_PROFILES_ACTIVE = 'demo'
    $env:MINERVA_DEMO_CATALOG_ENABLED = 'true'

    Push-Location $repoRoot
    try {
        & $mavenWrapper -f $pomFile spring-boot:run
        exit $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}
finally {
    $plainPassword = $null
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    foreach ($name in $managedEnvironment) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
}
