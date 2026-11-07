# Football ROI preprocessing fixture

All teams and all values are synthetic. League labels describe simulated groups, not real league statistics. Each row is one team-season. Five league groups, four teams per group, three seasons: 60 unique records; raw upload includes six exact duplicate rows (66 rows total). All money is in EUR, including the Saudi group; no foreign-exchange conversion is required. Match counts and dates follow one common simulation convention, not actual league calendars.

## Upload and oracle
- Upload ONLY `zd_football_roi_raw.csv`.
- `zd_football_roi_expected_clean.csv` is the comparison oracle; never upload it as a substitute for application processing.
- `expected_results.json` contains anomalies, tolerances, hashes and expected totals.
- Keep the previous baseline fixture; change the active football scenario's path rather than overwriting its contents.

## Required preprocessing
1. Remove six exact duplicate rows. The clean business key is `(team_id, season)`; `record_id` provides order-independent comparison.
2. Fill six blanks in `sponsorship_bonus_eur` with constant 0. The synthetic contract defines blank here as no bonus payment.
3. Fill four blanks in `average_attendance` with constant 25000. This value is known from this test fixture, not estimated from actual attendance.
4. Preserve identifiers as text, season as text, season_end_date as a date, and numeric fields as numbers.
5. Disable unrelated recommendations: outlier removal, encoding, binning, normalization, text transformations or extra imputation. Visit all seven tabs but do not apply those transformations.
6. If a required setting is not exposed by the actual UI, stop and report the specific unsupported rule. Do not silently choose mean/median or change the oracle.

Expected: 66 -> 60 rows, 28 columns, zero blank/null cells, all 60 IDs present exactly once. Missing cells are only in unique, nonduplicated rows. Financial inputs other than bonus are complete. Verify transformed cells and unchanged values against the golden CSV.

## Financial return definition
This is a custom seasonal financial-return metric for testing, not an audited club valuation or accounting ROI.

Revenue = broadcast + commercial + matchday + transfer sales + sponsorship bonus.
Cost base = wages + operating costs + transfer spend.
Net return = revenue - cost base.
ROI percentage = 100 * net return / cost base.

Transfer cash spending/sales are modeled directly; amortization, tax, financing and balance-sheet capital are outside this simplified simulation. Primary components are disjoint. Totals and ROI columns are precomputed numeric CSV values using clean inputs, not Excel formulas. Reconcile them again after application preprocessing; do not sum both components and totals as revenue.

`roi_pct=12.5` means 12.5 percent. Compare with a tolerance of 0.00001 percentage points. Group/overall ROI must use SUM(net_return_eur)/SUM(total_cost_eur)*100, never SUM(roi_pct) or an unweighted average.

Clean totals: revenue EUR 5,986,000,000; cost EUR 5,100,000,000; net return EUR 886,000,000; overall weighted ROI 17.372549019607842 percent. Negative and positive returns are valid and must be preserved.

## Later dashboard use
Dimensions: league, country, team_name, season. Measures: financial components, total revenue/cost/net return, ROI, points, wins, goals and attendance. Filter by season for within-season comparisons. SUM(points) across seasons means cumulative points, not a single-season ranking. Team financial comparisons can use weighted ROI, net return and costs alongside sporting performance. Dashboard creation is intentionally deferred.

## Current execution boundary
Login -> upload raw football CSV -> advanced preprocessing -> explicitly configure cleaning -> results -> save as new copy -> same original source count increases by one -> expand only after save -> exact processed version belongs to that source -> verify processed data against oracle -> UI logout -> verify login page.

No execution against the user's local application is claimed by this package. The supplied fixture was validated independently; Cursor/local authenticated runs must prove the application result.
