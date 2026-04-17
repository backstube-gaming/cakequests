param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Pattern,

    [Parameter(Position = 1)]
    [string[]]$Roots = @("decompiled-sources", "examples"),

    [int]$MaxResults = 120
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command rg -ErrorAction SilentlyContinue)) {
    throw "rg is required for scoped version API searches."
}

foreach ($root in $Roots) {
    if (Test-Path -LiteralPath $root) {
        $remaining = $MaxResults - $emitted
        if ($remaining -le 0) { break }

        $matches = @(rg --line-number --fixed-strings --glob "*.java" --glob "*.toml" --glob "*.json" --glob "*.mcmeta" --glob "*.snbt" $Pattern $root)
        $matches | Select-Object -First $remaining
        $emitted += [Math]::Min($matches.Count, $remaining)
    }
}
