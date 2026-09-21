# Phase 0 — Native formula map

Source HEAD: c5d6f3e196da617b291342e5ef3a3d0593139a80

| Rule | Input/condition | Exact formula/behavior | Web reference |
|---|---|---|---|
| Project scale | 100 project units = 1 m | meters = raw/100; m² = rawArea/10000 | webapp/index.html:L6000-L6006 |
| Wall integer input | 58 → 58 units = 0.58 m; 500 → 500 = 5.00 m | positive integer stays as units | webapp/index.html:L6866-L6890 |
| Wall decimal input | 0.58 → 58; 5.00 → 500 | decimal metres ×100 then Math.round | webapp/index.html:L6866-L6890 |
| Perimeter | sum measured edge.mm | m = raw perimeter /100 | webapp/index.html:L6898-L6905; webapp/index.html:L11822-L11858 |
| Rectangle recognition | 4-edge closed contour; drawn-corner \|cos\| <=0.22 | width=(s0+s2)/2; height=(s1+s3)/2; raw area=width×height | webapp/index.html:L11787-L11822 |
| General polygon area | solved point coordinates | shoelace absolute sum /2; display /10000 | webapp/index.html:L11857-L11905 |
| Field measurement tolerance | FIELD_MEASURE_TOL=10 project units = 0.10 m | free/orthogonal/diag solvers reject error above 10 | webapp/index.html:L6000-L6006; webapp/index.html:L11100-L11710 |
| Orthogonal closure tolerance | same as FIELD_MEASURE_TOL | horizontal and vertical closure residual each <=10 | webapp/index.html:L11640-L11660 |
| Diagonal tolerance | same as FIELD_MEASURE_TOL | max measured diagonal error <=10 | webapp/index.html:L11690-L11712 |
| Auto diagonal | solved positions | Math.hypot(b.x-a.x,b.y-a.y) | webapp/index.html:L11905-L11920 |
| Dowel quantity | 6 pcs per perimeter metre | Math.ceil(perimeterM*6) | webapp/index.html:L7247-L7295 |
| Canvas cost | areaM2 × canvas price | no extra rounding before display | webapp/index.html:L7247-L7295 |
| Baguette/insert | perimeterM × unit price | insert may be hidden; baguette fixed | webapp/index.html:L7247-L7295 |
| Extras/custom | qty × price | sum all enabled standard + custom items | webapp/index.html:L7247-L7295 |
| Order totals | sum room totals/area/perimeter | rooms loop; completeness propagated | webapp/index.html:L7296-L7335 |
| Money formatting | locale ru-RU, 0..2 decimals | toLocaleString | webapp/index.html:L6903-L6910 |
| Quantity formatting | locale ru-RU, default 2 decimals | fmtQty | webapp/index.html:L6903-L6910 |
| 4×500 regression | square sides all 500 | raw area 250000 → 25.00 m²; raw perimeter 2000 → 20 m | webapp/index.html:L11787-L11858 |

### Mandatory golden vectors
1. Square, four walls 500: **25.00 m²**, **20 m**.
2. Integer 58 displays as **0.58 m**, not 58 m.
3. Decimal 5,00 stores as **500 project units**.
4. Dowel count uses **ceil(perimeterM × 6)**, never floor/round.
5. A non-rectangular closed contour uses solver positions and shoelace; do not substitute a bounding-box area.

### Source-comment discrepancy
The inline comment near FIELD_MEASURE_TOL still mentions “5 = 0.05 m”, but the executable constant is **10** and all solver comparisons use that constant. Golden behavior is therefore **0.10 m tolerance**. The stale comment is a migration risk, not a formula.
