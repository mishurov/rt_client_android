package uk.co.mishurov.rtclient

import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.ByteOrder
import android.graphics.BitmapFactory
import android.graphics.Bitmap

import android.util.Log


class NetworkClient
{
    private var chanSend = DatagramChannel.open()
    private val bufSend = ByteBuffer.allocate(SEND_SIZE);
    private var chanRecv = DatagramChannel.open()
    private val bufRecv = ByteBuffer.allocate(PRIM_TYPE_SIZE + 1)

    private var isListening = false

    private var addr = InetAddress.getLoopbackAddress()
    private var isStereo = true

    var mono : Bitmap? = null
    var left : Bitmap? = null
    var right : Bitmap? = null


    fun listen(address: String, stereo: Boolean)
    {
        addr = InetAddress.getByName(address)
        isStereo = stereo

        if (!chanRecv.isOpen()) chanRecv = DatagramChannel.open()

        chanRecv.configureBlocking(false)
        chanRecv.socket().bind(InetSocketAddress(LISTEN_PORT))
        isListening = true

        Thread(
        {
            //Log.i(TAG, "listen")
            while (isListening) {
                bufRecv.clear()
                var res = chanRecv.receive(bufRecv)
                //if (res != null) Log.i(TAG, "UDP")
                if (res != null && bufRecv.position() == PRIM_TYPE_SIZE) {
                    bufRecv.flip()
                    val totalPack = bufRecv.order(
                        ByteOrder.LITTLE_ENDIAN
                    ).getInt()

                    val bufImg = ByteBuffer.allocate(totalPack * PACK_SIZE)
                    bufImg.clear()

                    for (i in 1..totalPack) {
                        res = null
                        while (res == null) {
                            res = chanRecv.receive(bufImg)
                        }
                        if (bufImg.position() % PACK_SIZE != 0) {
                            Log.i(TAG, "Received unexpected size pack")
                            continue
                        }
                    }

                    if (bufImg.position() != totalPack * PACK_SIZE)
                        continue

                    mono = BitmapFactory.decodeByteArray(
                        bufImg.array(), 0, bufImg.array().count()
                    )
                    if (isStereo && mono != null) {
                        val height = mono!!.getHeight()
                        val width = mono!!.getWidth()
                        val halfWidth = width / 2;
                        left = Bitmap.createBitmap(
                            mono, halfWidth, 0, halfWidth, height
                        )
                        right = Bitmap.createBitmap(
                            mono, 0, 0, halfWidth, height
                        )
                    }
                }
            }

            chanRecv.socket().close()

        }).start()
    }

    fun close()
    {
        isListening = false
    }

    fun packFloat(f: Float): Int
    {
        return java.lang.Integer.reverseBytes(
            java.lang.Float.floatToRawIntBits(f)
        )
    }

    fun packInt(i: Int): Int
    {
        return java.lang.Integer.reverseBytes(i)
    }

    fun send(w: Int, h: Int, interLens: Float, fovHoriz: Float, fovVert: Float,
                                up: FloatArray, view: FloatArray)
    {
        if (!chanSend.isOpen()) chanSend = DatagramChannel.open()
        bufSend.clear()

        // Up
        bufSend.putInt(packFloat(up[0]))
        bufSend.putInt(packFloat(up[1]))
        bufSend.putInt(packFloat(up[2]))
        // View
        bufSend.putInt(packFloat(view[0]))
        bufSend.putInt(packFloat(view[1]))
        bufSend.putInt(packFloat(view[2]))
        // Lens
        bufSend.putInt(packFloat(interLens))
        // Fov
        bufSend.putInt(packFloat(fovHoriz))
        bufSend.putInt(packFloat(fovVert))
        // Surface
        bufSend.putInt(packInt(w))
        bufSend.putInt(packInt(h))
        // Params
        bufSend.putInt(packInt(if (isStereo) 1 else 0))

        bufSend.flip()

        chanSend.send(bufSend, InetSocketAddress(addr, SEND_PORT))
    }

    companion object
    {
        private val TAG = "RtClient NetworkClient"

        private val SEND_PORT = 5000
        private val LISTEN_PORT = 5001

        private val PACK_SIZE = 4096
        private val SEND_COUNT = 12
        private val PRIM_TYPE_SIZE = 4
        private val SEND_SIZE = SEND_COUNT * PRIM_TYPE_SIZE
    }
}


