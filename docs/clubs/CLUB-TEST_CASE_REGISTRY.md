# CLUB Test Case Registry — US-FAP-CLUBS

| Test ID | AC | Title | Phase | Status | Test class/method | Notes |
|---------|-----|-------|-------|--------|-------------------|-------|
| CLUB-001 | Nav | Sidebar **الأندية** opens listing | a | Implemented | club001_navSidebar | |
| CLUB-002 | Title | Page heading **الأندية** | a | Implemented | club002_pageTitle | |
| CLUB-003 | Description | Yellow League browse description | a | Implemented | club003_description | |
| CLUB-004 | Cards | Club cards show names | b | Implemented | club004_clubCardsNames | |
| CLUB-005 | Logo | Logo optional on cards | b | Implemented | club005_logoOptional | |
| CLUB-006 | Select | Cards navigable (href / data-club-id) | b | Implemented | club006_cardsNavigable | |
| CLUB-007 | Identity | Detail URL contains stable **Club ID** | b | Implemented | club007_detailUrlClubId | |
| CLUB-008 | Read-only | No add / edit / delete on listing | a | Implemented | club008_readOnlyListing | |
| CLUB-009 | Empty | **لا توجد أندية متاحة حالياً** + description | c | Skip | - | needs empty data |
| CLUB-010 | Error | Load failure + **إعادة المحاولة** | c | Skip | - | needs failure hooks |

**Test class:** `com.taf.systems.zulfi.features.clubs.ClubsBrowseAcceptanceTest`

**Gate:** `com.taf.systems.zulfi.config.ClubsYellowLeagueGate` (`-Dclubs.yellow.phase=`)
