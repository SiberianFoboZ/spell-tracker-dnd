# =====================================================
# tools/generate_sql.ps1
# Converts D:\Downloads\CSV-File.csv (Windows-1251) into
# a SQL script that populates the `spell` table of the app.
#
# Run (PowerShell):
#   cd C:\Users\vk241\AndroidStudioProjects\Spelltracker
#   powershell -ExecutionPolicy Bypass -File tools\generate_sql.ps1
# =====================================================
# NOTE: This file is intentionally ASCII-only because PowerShell 5
# on Windows reads .ps1 files without BOM as ANSI and would mangle
# any Cyrillic characters in the source. The generated .sql file
# is still UTF-8 and contains Cyrillic data.

$ErrorActionPreference = "Stop"

$csvPath = "D:\Downloads\CSV-File.csv"
$outPath = Join-Path $PSScriptRoot "..\app\src\main\assets\databases\populate.sql"
$outDir  = Split-Path -Parent $outPath
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

# Read CSV in Windows-1251 encoding (Cyrillic)
$encoding = [System.Text.Encoding]::GetEncoding("windows-1251")
$content  = [System.IO.File]::ReadAllText($csvPath, $encoding)
$lines    = $content -split "`r?`n"

# Merge multi-line records by quote balance
function Merge-Records($lines) {
    $records = New-Object System.Collections.ArrayList
    $current = ""
    foreach ($line in $lines) {
        if ($current -eq "") { $current = $line } else { $current += "`n" + $line }
        $count = 0; $inQ = $false
        for ($i = 0; $i -lt $current.Length; $i++) {
            $c = $current[$i]
            if ($c -eq '"') {
                if ($inQ -and ($i + 1) -lt $current.Length -and $current[$i+1] -eq '"') {
                    $i++
                } else { $inQ = -not $inQ; $count++ }
            }
        }
        if ($count % 2 -eq 0) { [void]$records.Add($current); $current = "" }
    }
    if ($current -ne "") { [void]$records.Add($current) }
    return ,$records
}

# Parse one CSV record (delimiter ';', quotes "")
function Parse-CsvLine($line) {
    $fields = @(); $cur = ""; $inQ = $false
    for ($i = 0; $i -lt $line.Length; $i++) {
        $c = $line[$i]
        if ($inQ) {
            if ($c -eq '"') {
                if (($i + 1) -lt $line.Length -and $line[$i+1] -eq '"') { $cur += '"'; $i++ }
                else { $inQ = $false }
            } else { $cur += $c }
        } else {
            if     ($c -eq ';') { $fields += $cur; $cur = "" }
            elseif ($c -eq '"') { $inQ = $true }
            else                { $cur += $c }
        }
    }
    $fields += $cur
    return $fields
}

function Escape-Sql($s) {
    if ($null -eq $s) { return "''" }
    return "'" + ($s -replace "'", "''") + "'"
}

function Get-Field($arr, $idx) {
    if ($idx -ge $arr.Count) { return "" }
    return $arr[$idx]
}

Write-Host "Parsing CSV..."
$records = Merge-Records $lines
Write-Host ("Records after merging: " + $records.Count)

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("-- =====================================================")
[void]$sb.AppendLine("-- SQL script to populate the spell DB of the Spell Tracker app")
[void]$sb.AppendLine("-- Source: D:\Downloads\CSV-File.csv (Windows-1251)")
[void]$sb.AppendLine("-- Schema matches @Entity com.example.spelltracker.Spell")
[void]$sb.AppendLine("-- =====================================================")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("DROP TABLE IF EXISTS spell;")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("CREATE TABLE spell (")
[void]$sb.AppendLine("    id            INTEGER PRIMARY KEY AUTOINCREMENT,")
[void]$sb.AppendLine("    name          TEXT    NOT NULL,")
[void]$sb.AppendLine("    school        TEXT,")
[void]$sb.AppendLine("    level         INTEGER NOT NULL,")
[void]$sb.AppendLine("    casting_time  TEXT,")
[void]$sb.AppendLine("    range_text    TEXT,")
[void]$sb.AppendLine("    components    TEXT,")
[void]$sb.AppendLine("    duration      TEXT,")
[void]$sb.AppendLine("    description   TEXT,")
[void]$sb.AppendLine("    higher_level  TEXT")
[void]$sb.AppendLine(");")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("CREATE INDEX idx_spell_level ON spell(level);")
[void]$sb.AppendLine("CREATE INDEX idx_spell_name  ON spell(name COLLATE NOCASE);")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("BEGIN TRANSACTION;")
[void]$sb.AppendLine("")

$insertValues = @()
# Skip header (first record)
for ($i = 1; $i -lt $records.Count; $i++) {
    $fields = Parse-CsvLine $records[$i]
    if ($fields.Count -lt 16) { continue }

    $name = $fields[0].Trim()
    if ($name -eq "") { continue }

    $schoolAndLevel = $fields[1].Trim()
    $level = 0
    [int]::TryParse($fields[15].Trim(), [ref]$level) | Out-Null

    $school = ""
    $commaIdx = $schoolAndLevel.IndexOf(',')
    if ($commaIdx -ge 0) { $school = $schoolAndLevel.Substring($commaIdx + 1).Trim() }
    else                { $school = $schoolAndLevel }

    $castingTime = (Get-Field $fields 3)
    $range       = (Get-Field $fields 5)
    $components  = (Get-Field $fields 7)
    $duration    = (Get-Field $fields 9)
    $description = ((Get-Field $fields 11) -replace "`f", "`n").Trim()
    $higherLevel = ((Get-Field $fields 13) -replace "`f", "`n").Trim()

    $vals = @(
        (Escape-Sql $name),
        (Escape-Sql $school),
        $level,
        (Escape-Sql $castingTime),
        (Escape-Sql $range),
        (Escape-Sql $components),
        (Escape-Sql $duration),
        (Escape-Sql $description),
        (Escape-Sql $higherLevel)
    )
    $insertValues += "(" + ($vals -join ", ") + ")"
}

[void]$sb.Append("INSERT INTO spell (name, school, level, casting_time, range_text, components, duration, description, higher_level) VALUES`n")
[void]$sb.AppendLine(($insertValues -join ",`n"))
[void]$sb.AppendLine(";")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("COMMIT;")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("-- Total rows inserted: " + $insertValues.Count + ".")

# UTF-8 without BOM
$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($outPath, $sb.ToString(), $utf8)

Write-Host ("Generated: " + $outPath)
Write-Host ("INSERT rows: " + $insertValues.Count)
