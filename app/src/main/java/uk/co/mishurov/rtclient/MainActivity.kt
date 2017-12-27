package uk.co.mishurov.rtclient

import android.os.Bundle
import android.content.Intent
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.View
import android.view.MotionEvent
import android.view.GestureDetector
import android.content.SharedPreferences
import android.preference.PreferenceManager

import com.google.vr.sdk.base.AndroidCompat
import com.google.vr.sdk.base.Eye
import com.google.vr.sdk.base.GvrActivity
import com.google.vr.sdk.base.GvrView
import com.google.vr.sdk.base.HeadTransform
import com.google.vr.sdk.base.Viewport

import javax.microedition.khronos.egl.EGLConfig

import android.util.Log


class MainActivity : GvrActivity(),
                        GvrView.StereoRenderer,
                        View.OnTouchListener,
                        SharedPreferences.OnSharedPreferenceChangeListener
{
    private var mGestureDetector: GestureDetector? = null
    private var mSharedPreferences: SharedPreferences? = null
    private val client: NetworkClient = NetworkClient()
    private var shader: GlShader? = null
    private var textureId = 0
    private var width = 0
    private var height = 0
    private var fovHoriz = 45.0f
    private var fovVert = 45.0f
    private var interLens = 0.0f;
    // settings
    private var serverAddr = ""
    private var outputPref = 0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.common_ui)

        val gvrView = findViewById<GvrView>(R.id.gvr_view)
        gvrView.setRenderer(this)

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            AndroidCompat.setSustainedPerformanceMode(this, true)
        }
        setGvrView(gvrView)

        // preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mSharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        outputPref = mSharedPreferences!!.getString("output_preference", "").toInt()
        serverAddr = mSharedPreferences!!.getString("host_preference", "")

        if (outputPref == 2) gvrView.setStereoModeEnabled(false)
        if (outputPref != 0) gvrView.setDistortionCorrectionEnabled(false)

        gvrView.setOnTouchListener(this)
        interLens = gvrView.getGvrViewerParams().getInterLensDistance()

        mGestureDetector = GestureDetector(this, GestureListener())
    }

    override fun onPause()
    {
        gvrView?.onPause()
        client.close()
        super.onPause()
    }

    override fun onResume()
    {
        gvrView?.onResume()
        val isStereo = gvrView!!.getStereoModeEnabled()
        client.listen(serverAddr, isStereo)
        super.onResume()
    }

    override fun onNewFrame(ht: HeadTransform?)
    {
        //Log.i(TAG, "NewFrame")
        var up = FloatArray(3, { 0.0f })
        var view = FloatArray(3, { 0.0f })
        ht?.getUpVector(up, 0)
        ht?.getForwardVector(view, 0)

        client.send(width, height, interLens, fovHoriz, fovVert, up, view)
    }

    override fun onSurfaceChanged(w: Int, h: Int)
    {
        Log.i(TAG, "SurfaceChanged: " + w.toString() + " " + h.toString())
        width = w
        height = h
    }

    override fun onSurfaceCreated(conf: EGLConfig?)
    {
        textureId = GlUtil.createTexture(GLES20.GL_TEXTURE_2D)
        shader = GlShader(VERTEX_SHADER, FRAGMENT_SHADER)
        shader!!.useProgram()
    }

    override fun onDrawEye(eye: Eye?)
    {
        //Log.i(TAG, "onDrawEye")
        val fov = eye!!.getFov()
        fovHoriz = fov.getLeft() + fov.getRight()
        fovVert = fov.getTop() + fov.getBottom()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        shader!!.useProgram()
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        val stereo = gvrView!!.getStereoModeEnabled()
        if (stereo) {
            if (eye.getType() == Eye.Type.LEFT && client.left != null)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, client.right, 0);
            if (eye.getType() == Eye.Type.RIGHT && client.right != null)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, client.left, 0);
        } else {
            if (client.mono != null)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, client.mono, 0);
        }

        GLES20.glUniform1i(shader!!.getUniformLocation("img_tex"), 0)
        shader!!.setVertexAttribArray("position", 2, RECT_VERTICES)
        shader!!.setVertexAttribArray("in_tex", 2, RECT_TEX_COORDS)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun onFinishFrame(vp: Viewport?) {}

    override fun onRendererShutdown() {}

    override fun onSharedPreferenceChanged(sp: SharedPreferences, k: String)
    {
        when (k) {
            "output_preference" -> outputPref = sp.getString(k, "").toInt()
            "host_preference" -> serverAddr = sp.getString(k, "")
        }
    }

    override fun onTouch(v: View, me: MotionEvent): Boolean
    {
        return mGestureDetector?.onTouchEvent(me) as Boolean
    }

    private inner class GestureListener
                                : GestureDetector.SimpleOnGestureListener()
    {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val intent = Intent(
                    this@MainActivity,
                    SettingsActivity::class.java
            )
            startActivity(intent)
            return true
        }
    }


    companion object
    {
        private val TAG = "RtCLient MainActivity"

        private val VERTEX_SHADER = "attribute vec4 position;\n" +
                "attribute vec4 in_tex\n;" +
                "varying vec2 out_tex\n;" +
                "void main() {\n" +
                "	gl_Position = position;\n" +
                "	out_tex = in_tex.xy;\n" +
                "}\n"
        private val FRAGMENT_SHADER = "precision mediump float;\n" +
                "varying vec2 out_tex;\n" +
                "\n" +
                "uniform sampler2D img_tex;\n" +
                "\n" +
                "void main() {\n" +
                "	gl_FragColor = texture2D(img_tex, out_tex);\n" +
                "}\n"

        private val RECT_VERTICES = GlUtil.createFloatBuffer(
            floatArrayOf(-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f)
        )
        private val RECT_TEX_COORDS = GlUtil.createFloatBuffer(
            floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)
        )
    }
}


