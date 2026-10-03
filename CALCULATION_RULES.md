# Calculation rules

UI unit text is мм. Business scale is deliberately **100 entered units = 1 m**, and **10000 raw area units = 1 m²**. Four 500 sides in a square produce **25 m², 20 m**. Do not convert by 1000/1000000.

NativeEstimateEngine.metrics supplies room metrics. Missing positive side lengths make the perimeter incomplete. Area requires a supported valid closed geometry. Project totals aggregate rooms. Automatic insert equals applicable room perimeter, default price zero; a manual override remains until explicitly returned to automatic. Dowels are ceil(perimeter metres × 6). Currency is entered by the user; line total is quantity × price. Exports include only used/selected items, never empty category placeholders.

Formula changes require explicit user authorization and updated regression evidence. UI displays results; it does not calculate them.
