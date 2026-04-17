param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Pattern,

    [string[]]$Roots = @(
        "examples/FTB-Quests-1.18-main",
        "examples/FTB-Library-1.18-main",
        "examples/ATM-8-main/ftbquests/quests",
        "src"
    ),

    [int]$MaxResults = 120
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command rg -ErrorAction SilentlyContinue)) {
    throw "rg is required for scoped quest reference searches."
}

foreach ($root in $roots) {
    if (Test-Path -LiteralPath $root) {
        $remaining = $MaxResults - $emitted
        if ($remaining -le 0) { break }

        $matches = @(rg --line-number --fixed-strings --glob "*.java" --glob "*.snbt" --glob "*.json" --glob "*.toml" $Pattern $root)
        $matches | Select-Object -First $remaining
        $emitted += [Math]::Min($matches.Count, $remaining)
    }
}
