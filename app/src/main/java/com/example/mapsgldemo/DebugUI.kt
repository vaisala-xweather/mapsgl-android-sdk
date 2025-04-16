package com.example.mapsgldemo

import com.xweather.mapsgl.map.MapController
import com.xweather.mapsgl.map.mapbox.MapboxMapController

class DebugUI {
        companion object{
            fun updateDisplayInfo(mapController: MapController): String {
                //return ""
                val coordinateBounds1 = mapController.getBounds/*Custom*/()
                // This is for debugging, to it is not optimized. String concat is SLOW.



                return      "Zoom:${String.format("%.2f", mapController.getZoom())}\n " +
                  "Ln: ${String.format("%.3f", mapController.getCenter().lon)}" + " Lt: ${String.format("%.3f", mapController.getCenter().lat)} \n" //+
                  //"matrix val:  ${(mapController as MapboxMapController).matrixVal}"
                   //"Span UW:${String.format("%.1f", (mapController as MapboxMapController).getMapboxBounds().longitudeSpan())} \n" +
                    //"Crosses AM:${(mapController).getMapboxBounds().crossesAntimeridian()} \n" +
                    //    "W:${String.format("%.1f", coordinateBounds1.westExtent)} " +
                    //    "E:${String.format("%.1f", coordinateBounds1.eastExtent)} \n" +
                    //    "N:${String.format("%.1f", coordinateBounds1.northExtent)} " +
                    //    "S:${String.format("%.1f", coordinateBounds1.southExtent)} "

            }
        }

        /** Used to toggle between display states for the floating info display */


}
