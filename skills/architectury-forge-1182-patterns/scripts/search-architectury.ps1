param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Pattern,

    [string[]]$Roots = @(
        "examples/architectury-api-1.18.2",
        "examples/FTB-Quests-1.18-main",
        "decompiled-sources/net/minecraftforge",
        "src"
    ),

    [int]$MaxResults = 120
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command rg -ErrorAction SilentlyContinue)) {
    throw "rg is required for scoped Architectury searches."
}

foreach ($root in $roots) {
    if (Test-Path -LiteralPath $root) {
        $remaining = $MaxResults - $emitted
        if ($remaining -le 0) { break }

        $matches = @(rg --line-number --fixed-strings --glob "*.java" --glob "*.gradle" --glob "*.toml" --glob "*.json" $Pattern $root)
        $matches | Select-Object -First $remaining
        $emitted += [Math]::Min($matches.Count, $remaining)
    }
}
