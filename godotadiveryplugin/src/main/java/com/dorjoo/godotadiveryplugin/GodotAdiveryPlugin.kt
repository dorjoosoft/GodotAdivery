package com.dorjoo.godotadiveryplugin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.collection.ArraySet
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener
import com.adivery.sdk.AdiveryNativeCallback
import com.adivery.sdk.NativeAd
import com.adivery.sdk.networks.adivery.AdiveryNativeAd
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotLib
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import java.io.ByteArrayOutputStream
import java.util.*


class GodotAdiveryPlugin(activity: Godot) : GodotPlugin(activity) {

    var nativeAdResponseId: String = ""

    var resDict = Dictionary()

    fun sendNativeToGame(){
        emitSignal("native_ad_filled", resDict)
    }

    fun nativeAdClicked(zoneID: String, adId: String){
        nativeAdClicked(zoneID, adId)
    }


    override fun getPluginName(): String {
        // Plugin name
        return "GodotAdiveryPlugin"
    }

    override fun getPluginMethods(): List<String> {
        // Available plugin functions to use in Godot
        return listOf("initPlugin", "prepareRewardedAd" , "prepareInterstitialAd" ,
            "showRewardVideoAd", "showInterstitialAd" , "requestAd", "requestNativeAd",
            "nativeAdClicked")
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = ArraySet()
        signals.add(SignalInfo("onRewardedAdClosed", String::class.java))
        signals.add(SignalInfo("onInterstitialAdLoaded", String::class.java))
        signals.add(SignalInfo("on_opened"))
        signals.add(SignalInfo("on_closed"))
        signals.add(SignalInfo("on_expiring"))
        signals.add(SignalInfo("on_no_ad_available"))
        signals.add(SignalInfo("on_error", String::class.java))
        signals.add(SignalInfo("on_no_network"))
        signals.add(SignalInfo("onNativeAdFilled", Dictionary::class.java))
        //signals.add(SignalInfo("permission_not_granted_by_user", String::class.java))

        return signals
    }

    fun initPlugin(){

        Adivery.configure(this.activity?.application!!,  GodotLib.getGlobal("adivery/adivery_key"))

        Adivery.addGlobalListener(object : AdiveryListener() {
            override fun onRewardedAdLoaded(placementId: String) {
                // تبلیغ جایزه‌ای بارگذاری شده
            }
            override fun onRewardedAdClosed(placementId: String, isRewarded: Boolean) {
                // بررسی کنید که آیا کاربر جایزه دریافت می‌کند یا خیر
                emitSignal("onRewardedAdClosed", isRewarded.toString())
            }
            override fun onInterstitialAdLoaded(placementId: String) {
                // تبلیغ میان‌صفحه‌ای بارگذاری شده
                emitSignal("onInterstitialAdLoaded", placementId)
            }

        })

    }

    fun prepareRewardedAd(placementId: String ){
        Adivery.prepareRewardedAd(godot.requireContext(), placementId);
    }

    fun prepareInterstitialAd(placementId: String ){
        Adivery.prepareInterstitialAd(godot.requireContext(), placementId);
    }

    fun requestNativeAd(placementId: String) {
        Adivery.requestNativeAd(godot.requireContext() , placementId , object : AdiveryNativeCallback() {
            override fun onAdLoaded(ad: NativeAd) {
                val resDict = Dictionary()
                if (ad is AdiveryNativeAd) {
                    resDict["headline"] = ad.headline
                    resDict["description"] = ad.description
                    resDict["advertiser"] = ad.advertiser
                    resDict["call_to_action"] = ad.callToAction
                    val icon = drawableToBitmap(ad.icon)
                    if (icon != null) {
                        resDict["icon"] = encodeToBase64(icon, Bitmap.CompressFormat.JPEG, 100)
                    } else {
                        resDict["icon"] = "null"
                    }
                    val image = drawableToBitmap(ad.image)
                    if (image != null) {
                        resDict["image"] = encodeToBase64(image, Bitmap.CompressFormat.JPEG, 100)
                    } else {
                        resDict["image"] = "null"
                    }
                    val id = UUID.randomUUID().toString()
                    resDict["id"] = id
                    emitSignal("onNativeAdFilled", resDict)
                }
            }
        })}

    fun encodeToBase64(
        image: Bitmap,
        compressFormat: Bitmap.CompressFormat?,
        quality: Int
    ): String? {
        val byteArrayOS = ByteArrayOutputStream()
        image.compress(compressFormat, quality, byteArrayOS)
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.NO_WRAP)
    }

    fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }
        var bitmap: Bitmap? = null
        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }
        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    var rewardedResponseId = "-1"
    fun requestAd(zoneID: String) {

    }

    fun showNativeAd(placementId: String) {
            Adivery.showAd(placementId)
    }

    fun showRewardVideoAd(PlacementId: String) {
        runOnUiThread{
            if (Adivery.isLoaded(PlacementId)){
                Adivery.showAd(PlacementId);
            }
        }

    }

    fun showInterstitialAd(PlacementId: String) {
        runOnUiThread{
            if (Adivery.isLoaded(PlacementId)){
                Adivery.showAd(PlacementId);
            }
        }

    }

    override fun onMainActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}

    override fun onMainPause() {}

    override fun onMainResume() {}

    override fun onMainDestroy() {
        return
    }
}