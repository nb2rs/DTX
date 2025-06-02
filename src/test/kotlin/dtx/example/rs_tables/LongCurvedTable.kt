package dtx.example.rs_tables

import dtx.example.Item

val longCurvedTable = rsWeightedTable {
    name("Long and Curved Bone table")
    400 weight Item("long_bone")
    1 weight Item("curved_bone")
}