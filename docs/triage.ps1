$ErrorActionPreference = 'Stop'
$issues = Get-Content -Raw C:/Users/jucmn/Claude/jUCMNav2026/legacy-issues-raw.json | ConvertFrom-Json

# Word-boundary regex patterns (case-insensitive). Specificity over recall:
# only flag when an issue title/body strongly suggests it overlaps a known
# modernization-branch change. Bias toward false negatives so triage time
# is not wasted on unrelated bugs that happen to share a word.
$touched_keywords = @{
    'icons/Metadata.gif missing'        = @('\bmetadata\.gif\b','add\s+stereotype\s+definition')
    'JDK 21 DateFormat LONG parse'      = @('unparseable\s+date','reportgendate')
    'HTML report Invalid thread access' = @('invalid\s+thread\s+access.*html','html.*invalid\s+thread')
    'HTML report frameset / XSLT'       = @('\bxslt\b','xmltree','\bframeset\b','xsltprocessor')
    'HTML report leaf naming / menu'    = @('html.*menu.*name','html.*tree\.xml','tree\.xml')
    'HTML report file lock / empty gif' = @('feature16\.gif','pages.*feature16','locked.*pages')
    'Save As .jucm extension'           = @('save\s+as.*extension','wrong\s+extension','no\s+\.jucm\b','reopen.*text\s+editor')
    'PathNodeEditPart undo NPE'         = @('splitlinkcommand','splitlink\s+undo','undo.*nullpointer','nullpointer.*undo')
    'GRL evaluation labels not scaling' = @('label.*not\s+scal','evaluation.*not\s+scal','kpi.*label.*scal','grl.*label.*zoom')
    'PDF/RTF SWTGraphics leak'          = @('swtgraphics\s+leak','pdf.*swt\s+resource','rtf.*swt\s+resource','transform\s+leak')
    'GRL evaluation perf'               = @('static\s+slic.*perf','slicing\s+perf','grl\s+evaluation\s+slow')
    'Z.151 Belief author/size'          = @('belief\s+author','belief\s+size','z\.?151.*belief')
    'JAX-RPC KPI Web Services'          = @('\bjax-rpc\b','kpi\s+web\s+service')
    'Comment editpart dispose race'     = @('commenteditpart','comment.*dispose|graphic\s+is\s+disposed.*comment')
    'MSC viewer font / paint crash'     = @('jucmscenarios','msc\s+viewer.*paint','msc\s+viewer.*crash','ucmscenarioviewer','msc.*font.*crash','msc.*invalid')
    'Stub icons in HTML sidebar'        = @('stub.*icon.*report','html.*stub.*icon')
    'doSaveAs disposed-site error'      = @('getshell.*disposed','partsite.*disposal','disposed.*save')
}

# Coarse area buckets for the report
$area_rules = @(
    @{ pattern = 'html.*report|html.*output|html.*export|tree\.xml|xmltree|xsl|menu\.html'; area = 'HTML report' }
    @{ pattern = 'msc|jucmscenarios|scenario viewer';                                       area = 'MSC scenario viewer' }
    @{ pattern = 'pdf report|rtf report|pdf export|rtf export';                              area = 'PDF/RTF reports' }
    @{ pattern = 'scenario|traversal|scenariodef';                                            area = 'Scenarios' }
    @{ pattern = 'grl|goal|softgoal|evaluation strategy|strategy|kpi|indicator';              area = 'GRL/KPI' }
    @{ pattern = 'feature diagram|featuremodel|fmd|fm diagram';                              area = 'Feature model' }
    @{ pattern = 'stub|plug-in binding|binding|dynamic stub';                                area = 'UCM stubs / bindings' }
    @{ pattern = 'clipboard|copy|paste|edit-copy';                                            area = 'Clipboard / copy' }
    @{ pattern = 'palette|toolbar|menu bar|context menu|action';                              area = 'UI / actions / palette' }
    @{ pattern = 'metadata|stereotype|tag|annotation';                                        area = 'Metadata / stereotypes' }
    @{ pattern = 'save|saveas|load|file|persist|xmi|emf';                                     area = 'Save / load / persistence' }
    @{ pattern = 'tdl|z\.?151|import|export';                                                 area = 'Import / export (Z.151/TDL)' }
    @{ pattern = 'properties|tabbed|propertysheet|view';                                      area = 'Views / properties' }
    @{ pattern = 'concern|aspect|aourn';                                                      area = 'Concerns / AoURN' }
    @{ pattern = 'zoom|outline|figure|paint|render|draw2d';                                   area = 'Rendering / GEF' }
    @{ pattern = 'build|maven|tycho|p2|update site';                                          area = 'Build / packaging' }
    @{ pattern = 'documentation|wiki|tutorial|readme';                                        area = 'Docs' }
    @{ pattern = 'test|junit';                                                                area = 'Tests' }
)

function Get-Area($title, $body) {
    $text = ("$title $body").ToLower()
    foreach ($r in $area_rules) {
        if ($text -match $r.pattern) { return $r.area }
    }
    return 'Other / uncategorized'
}

function Get-Likely-Fixed($title, $body) {
    $text = ("$title $body").ToLower()
    $hits = @()
    foreach ($k in $touched_keywords.Keys) {
        foreach ($kw in $touched_keywords[$k]) {
            if ([regex]::IsMatch($text, $kw, 'IgnoreCase')) { $hits += $k; break }
        }
    }
    return ($hits | Select-Object -Unique)
}

function Get-Disposition($title, $body) {
    $text = ("$title $body").ToLower()
    if ($text -match 'feature request|would be nice|enhancement|suggestion|wish') { return 'Enhancement (defer)' }
    if ($text -match 'crash|exception|nullpointerexception|stack trace|error|fails|broken') { return 'Bug - needs repro' }
    if ($text -match 'documentation|tutorial|how to|how do i|question') { return 'Question / docs' }
    return 'Needs review'
}

$rows = foreach ($i in $issues) {
    $title = if ($i.title) { $i.title } else { '' }
    $body  = if ($i.body)  { $i.body  } else { '' }
    $area  = Get-Area $title $body
    $disp  = Get-Disposition $title $body
    $fixed = Get-Likely-Fixed $title $body
    $fixed_str = if ($fixed.Count -gt 0) { '[?] Likely fixed by: ' + ($fixed -join '; ') } else { '' }
    [pscustomobject]@{
        Number       = $i.number
        Area         = $area
        Disposition  = $disp
        LikelyFixed  = $fixed_str
        Title        = $title
        Created      = ([datetime]$i.created_at).ToString('yyyy-MM-dd')
        URL          = $i.html_url
    }
}

$total = $rows.Count
$by_area = $rows | Group-Object Area | Sort-Object Count -Descending
$by_disp = $rows | Group-Object Disposition | Sort-Object Count -Descending
$fixed_count = ($rows | Where-Object { $_.LikelyFixed }).Count

$sb = New-Object Text.StringBuilder
[void]$sb.AppendLine('# Legacy issue triage (open issues on `JUCMNAV/projetseg-update`)')
[void]$sb.AppendLine('')
[void]$sb.AppendLine("Snapshot of $total open issues at the time of repo transfer (damyot/jUCMNavPlus -> JUCMNAV/jUCMNavPlus).")
[void]$sb.AppendLine('Source: <https://github.com/JUCMNAV/projetseg-update/issues?q=is%3Aissue+is%3Aopen>')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('## Triage summary')
[void]$sb.AppendLine('')
[void]$sb.AppendLine("- Total open: **$total**")
[void]$sb.AppendLine("- Flagged as possibly already fixed by ``modernization``: **$fixed_count** (need verification before opening fresh issues)")
[void]$sb.AppendLine('')
[void]$sb.AppendLine('### By area')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('| Area | Open |')
[void]$sb.AppendLine('|---|---|')
foreach ($g in $by_area) {
    [void]$sb.AppendLine("| $($g.Name) | $($g.Count) |")
}
[void]$sb.AppendLine('')
[void]$sb.AppendLine('### By disposition (heuristic)')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('| Disposition | Open |')
[void]$sb.AppendLine('|---|---|')
foreach ($g in $by_disp) {
    [void]$sb.AppendLine("| $($g.Name) | $($g.Count) |")
}
[void]$sb.AppendLine('')

[void]$sb.AppendLine('## "Possibly fixed by modernization" - verify first')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('These issues match keywords from areas the modernization branch already touched. Spot-check on the latest `master` before deciding to re-file or close as fixed.')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('| # | Area | Title | Likely fixed by | Created |')
[void]$sb.AppendLine('|---|---|---|---|---|')
foreach ($r in ($rows | Where-Object { $_.LikelyFixed } | Sort-Object Number -Descending)) {
    $safe_title = ($r.Title -replace '\|','\|')
    $safe_fixed = (($r.LikelyFixed -replace '\|','\|') -replace '^\[\?\] Likely fixed by: ','')
    [void]$sb.AppendLine("| [#$($r.Number)]($($r.URL)) | $($r.Area) | $safe_title | $safe_fixed | $($r.Created) |")
}
[void]$sb.AppendLine('')

[void]$sb.AppendLine('## Full list by area')
[void]$sb.AppendLine('')
foreach ($g in ($by_area | Sort-Object Name)) {
    [void]$sb.AppendLine("### $($g.Name) ($($g.Count))")
    [void]$sb.AppendLine('')
    [void]$sb.AppendLine('| # | Disposition | Title | Created |')
    [void]$sb.AppendLine('|---|---|---|---|')
    foreach ($r in ($rows | Where-Object { $_.Area -eq $g.Name } | Sort-Object Number -Descending)) {
        $safe_title = ($r.Title -replace '\|','\|')
        $fixed_marker = if ($r.LikelyFixed) { ' [?]' } else { '' }
        [void]$sb.AppendLine("| [#$($r.Number)]($($r.URL)) | $($r.Disposition)$fixed_marker | $safe_title | $($r.Created) |")
    }
    [void]$sb.AppendLine('')
}

[void]$sb.AppendLine('## Recommended next steps')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('1. Walk the "Possibly fixed by modernization" table above. For each row, install the current `modernization` build and try the original repro. If the bug is gone, leave a comment on the legacy issue pointing at the fixing commit on `JUCMNAV/jUCMNavPlus` and close as obsolete. (You have admin on `JUCMNAV/projetseg-update`, so you can close directly.)')
[void]$sb.AppendLine('2. For surviving bugs, use GitHub''s "Transfer issue" feature on the legacy issue (same-org transfers work natively now that both repos live under `JUCMNAV/`). Preserves comments, authors, and cross-references. Add an `area:*` label after transfer.')
[void]$sb.AppendLine('3. Enhancements / questions / docs items: leave on `projetseg-update` for the archive. Cherry-pick to the new tracker only when someone actually plans to work on them.')
[void]$sb.AppendLine('4. Re-run this triage (`triage.ps1` in the repo root of `jUCMNav2026`) periodically as the legacy issue list shrinks.')

$outPath = 'C:/Users/jucmn/Claude/jUCMNav2026/jUCMNavPlus/docs/legacy-issue-triage.md'
$outDir = Split-Path $outPath
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
$sb.ToString() | Out-File -FilePath $outPath -Encoding utf8

"Wrote $outPath"
"  rows: $total"
"  flagged-possibly-fixed: $fixed_count"
