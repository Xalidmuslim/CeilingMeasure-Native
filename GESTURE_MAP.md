# Gesture map

Shared policy: 330 ms long press; cancel on meaningful movement or extra pointer. Two pointers own pinch and pan, centered on their centroid; one remaining pointer cannot draw. Scale is 0.45–12. No gesture changes measurement IDs.

| Context | Tap | Hold | Single drag | Two fingers |
|---|---|---|---|---|
| Wall, open | select existing target | edit measured wall/diagonal; vertex actions | one wall from endpoint | viewport |
| Wall, closed | select existing target | edit | empty canvas scrolls | viewport |
| Points | select vertex | edit measurement / vertex actions | move selected vertex | viewport |
| Diagonal | pick two existing vertices | edit/delete diagonal | diagonal draft | viewport |
| Numbers | select label | edit dimension | move/rotate explicit handle | viewport |
| Room card | open | rename/delete menu | horizontal list; vertical page | platform scrolling |
| Material | price control | edit quantity | page scroll | platform scrolling |

Delete/clear require explicit confirmation. Right scroll strip remains available before contour closure. Temporary draft state is cancelled on ACTION_CANCEL and never becomes an accidental committed wall.
