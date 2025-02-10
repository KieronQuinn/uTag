package com.kieronquinn.app.utag.xposed.extensions

/**
 *  Checks whether this is the standalone module by looking for a class that's only included in
 *  the standalone module.
 */
fun isStandaloneModule(): Boolean {
    return try {
        Class.forName("com.kieronquinn.app.utag.xposed.Standalone")
        true
    }catch (e: ClassNotFoundException) {
        false
    }
}