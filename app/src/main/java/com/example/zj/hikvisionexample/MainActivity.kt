package com.example.zj.hikvisionexample

import android.graphics.PixelFormat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.MediaPlayer.PlayM4.Player
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
* Created by zj on 2018/7/10.
* is use for:  海康威视测试界面
 * 初始化sdk，登录用户，开启预览(因jar包问题暂时无法成功提供预览界面，但可开启预览)，设备抓图(直接抓图)
*/

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private val TAG = "main"
    private var m_iPort:Int =-1
    private var logID:Int =-1
    private var m_iStartChan = 0
    private var m_iChanNum = 0
    private val strIP = ""
    private val nPort = 0
    private val strUser = ""
    private val strPsd = ""//分别输入你登录用的信息

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        Log.e(TAG, "surface is created$m_iPort")
        if (-1 == m_iPort) {
            return
        }
        val surface = holder?.surface
        if (true == surface?.isValid) {
            if (!Player.getInstance().setVideoWindow(m_iPort, 0, holder)) {
                Log.e(TAG, "Player setVideoWindow failed!")
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {

    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (-1 == m_iPort) {
            return
        }
        if (true == holder?.surface?.isValid) {
            if (!Player.getInstance().setVideoWindow(m_iPort, 0, null)) {
                Log.e(TAG, "Player setVideoWindow failed!")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("m_iPort", m_iPort)
        super.onSaveInstanceState(outState)
        Log.i(TAG, "onSaveInstanceState")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        m_iPort = savedInstanceState.getInt("m_iPort")
        super.onRestoreInstanceState(savedInstanceState)
        Log.i(TAG, "onRestoreInstanceState")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //海康设备初始化
        val initSDK: Boolean = HikVisionUtils.getInstance().initSDK()
        Log.i(TAG, initSDK.toString() + "注册设备")

        surfaceView.holder.addCallback(this)

        //海康登录
        logID = HikVisionUtils.getInstance().loginNormalDevice(strIP,nPort,strUser,strPsd)
        m_iStartChan = HikVisionUtils.m_iStartChan
        m_iChanNum = HikVisionUtils.m_iChanNum

        //海康开启预览(只有成功开启预览才能抓到需要的图片,此处不展示预览功能，故默认直接调用函数开启)
        HikVisionUtils.getInstance().startSinglePreview(logID,m_iStartChan,m_iPort)

        btnCapture.setOnClickListener({
            try {
                // 创建文件目录，文件
                var sdf = SimpleDateFormat("yyyy-MM-dd-hh:mm:ss")
                val date = Date()
                val fileNameString = "/sdcard/Download/test/$date.jpg"
                val file = File(fileNameString)
                //抓图
                val b:Boolean = HikVisionUtils.getInstance().capture(logID,m_iChanNum,fileNameString)
                if (b){
                    Toast.makeText(this,"成功抓图，请到对应文件夹处查看",Toast.LENGTH_LONG).show()
                }
            } catch (err: Exception) {
                Log.e(TAG, "error: " + err.toString())
            }
        })
    }
}
