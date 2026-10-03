# Geometry rules

NativeGeometry owns a graph with stable vertex/edge IDs. A single closed cycle is required for room area. A simple valid polygon excludes crossings, duplicate edges and zero-length sides. Straight continuation extends the endpoint rather than introducing an artificial corner.

NativeMeasurementSolver owns all measured reconstruction. It keeps factual geometry distinct from the stored original sketch. Measurements and manually measured diagonals are constraints; automatic diagonal display is derived data. A square/orthogonal sketch is an explicit geometric model; arbitrary quadrilateral side lengths alone do not determine area. Missing rigidity or inconsistent measurements must be reported rather than silently applied.

Factual conflict threshold: 1 entered unit. Legacy working tolerance remains 10 entered units only where existing nonfactual calculation uses it; cosmetic changes cannot change either. Re-editing measurements marks factual geometry stale. Returning to sketch restores coordinates and retains entered dimensions.

Labels are keyed by edge/diagonal ID. Placement never reorders measurements or changes graph endpoints. View transforms are independent of world coordinates.
