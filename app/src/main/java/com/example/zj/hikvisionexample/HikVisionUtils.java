package com.example.zj.hikvisionexample;

import android.util.Log;

import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.NET_DVR_CLIENTINFO;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_JPEGPARA;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.RealPlayCallBack;

import org.MediaPlayer.PlayM4.Player;

import java.nio.ByteBuffer;

/**
 * Created by zj on 2018/6/28.
 * is use for:  海康威视 工具类
 */

public class HikVisionUtils {
    private static final String TAG = "main";
    private static HikVisionUtils mInstance = null;
    private int logID =-1;
    private NET_DVR_DEVICEINFO_V30 m_oNetDvrDeviceInfoV30;
    public static int m_iStartChan = 0;
    public static int m_iChanNum = 0;
    private int 	m_iPlayID= -1;
    private int m_iPort = -1;
    private int m_iPlaybackID = -1;

    public static HikVisionUtils getInstance() {

        if (mInstance == null) {
            synchronized (HikVisionUtils.class) {
                if (mInstance == null) {
                    mInstance = new HikVisionUtils();
                }
            }
        }
        return mInstance;
    }

    /**
     * 初始化sdk
     *
     * @return true:成功  false:失败
     */
    public boolean initSDK() {
        Boolean isNetDVRInit = HCNetSDK.getInstance().NET_DVR_Init();
        if (!isNetDVRInit) {
            Log.e(TAG, "初始化失败" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return false;
        }
        String logFilePath = "/mnt/sdcard/sdklog/";
        //nLogLevel[in] 日志的等级（默认为0）：
        // 0-表示关闭日志，1-表示只输出ERROR错误日志，
        // 2-输出ERROR错误信息和DEBUG调试信息，
        // 3-输出ERROR错误信息、DEBUG调试信息和INFO普通信息等所有信息
        int logLevel = 3;
        HCNetSDK.getInstance().NET_DVR_SetLogToFile(logLevel, logFilePath, true);
        return true;
    }

    /**
     * 设备登录
     *
     * @param address  地址
     * @param port     端口
     * @param user     用户名
     * @param password 密码
     * @return 结果
     */
    public int loginNormalDevice(String address, int port, String user, String password) {
        try {
            if (logID< 0) {
                // login on the device
                logID = loginDevice(address,port,user,password);
                if (logID < 0) {
                    Log.e(TAG, "登录失败");
                    return -1;
                }
                // get instance of exception callback and set
                ExceptionCallBack oexceptionCbf = getExceptiongCbf();
                if (oexceptionCbf == null) {
                    Log.e(TAG, "ExceptionCallBack object is failed!");
                    return -1;
                }

                if (!HCNetSDK.getInstance().NET_DVR_SetExceptionCallBack(oexceptionCbf)) {
                    Log.e(TAG, "NET_DVR_SetExceptionCallBack is failed!");
                    return -1;
                }
            } else {
                Log.e(TAG,"退出登陆");
                // whether we have logout
                if (!HCNetSDK.getInstance().NET_DVR_Logout_V30(logID)) {
                    Log.e(TAG, " NET_DVR_Logout is failed!");
                    return -1;
                }
                logID = -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "error: " + e.toString());
        }
        return logID;
    }

    private int loginDevice(String address, int port, String user, String password) {
        m_oNetDvrDeviceInfoV30 = new NET_DVR_DEVICEINFO_V30();
        if (null == m_oNetDvrDeviceInfoV30) {
            Log.e(TAG, "获取设备信息失败");
            return -1;
        }

        int iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(address, port, user, password, m_oNetDvrDeviceInfoV30);
        if (iLogID < 0) {
            Log.e(TAG, "登录失败错误码为:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return -1;
        }
        if (m_oNetDvrDeviceInfoV30.byChanNum > 0){
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byChanNum;
        }else{
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byIPChanNum + m_oNetDvrDeviceInfoV30.byHighDChanNum * 256;
        }
        Log.i(TAG, "登录成功");

        return iLogID;
    }

    private ExceptionCallBack getExceptiongCbf()
    {
        ExceptionCallBack oExceptionCbf = new ExceptionCallBack()
        {
            public void fExceptionCallBack(int iType, int iUserID, int iHandle)
            {
                System.out.println("recv exception, type:" + iType);
            }
        };
        return oExceptionCbf;
    }

    /**
     * 开启预览
     * @param logID
     * @param m_iStartChan
     */
    public void startSinglePreview(int logID,int m_iStartChan,int port) {
        {
            m_iPort = port;
            //处理真实数据所在
            RealPlayCallBack fRealDataCallBack = getRealPlayerCbf();
            if (fRealDataCallBack == null) {
                Log.e(TAG, "fRealDataCallBack object is failed!");
                return;
            }
            Log.i(TAG, "m_iStartChan:" + m_iStartChan);

            NET_DVR_PREVIEWINFO previewInfo = new NET_DVR_PREVIEWINFO();
            previewInfo.lChannel = m_iStartChan;//通道号
            previewInfo.dwStreamType = 1; //码流协议
            previewInfo.bBlocked = 1; //是否阻塞取流
            previewInfo.byPreviewMode = 0; //预览模式

            NET_DVR_CLIENTINFO clientinfo = new NET_DVR_CLIENTINFO();
            clientinfo.lChannel = m_iStartChan;

            // HCNetSDK start preview
            m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(logID, previewInfo, fRealDataCallBack);
//        HCNetSDK.getInstance().NET_DVR_RealPlay_V30(m_iLogID, clientinfo, fRealDataCallBack,true);
            if (m_iPlayID < 0) {
                Log.e(TAG, "NET_DVR_RealPlay is failed!Err:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
                return;
            }

            Log.i(TAG, "NetSdk Play sucess ***********************3***************************");
        }
    }

    private RealPlayCallBack getRealPlayerCbf(){
        return (iRealHandle, iDataType, pDataBuffer, iDataSize) -> {
            // player channel 1
            processRealData(1, iDataType, pDataBuffer, iDataSize, Player.STREAM_REALTIME);
        };
    }

    public void processRealData(int iPlayViewNo, int iDataType, byte[] pDataBuffer, int iDataSize, int iStreamMode)
    {
        {
            if(HCNetSDK.NET_DVR_SYSHEAD == iDataType)
            {
                if(m_iPort >= 0)
                {
                    return;
                }
                m_iPort = Player.getInstance().getPort();
                if(m_iPort == -1)
                {
                    Log.e(TAG, "getPort is failed with: " + Player.getInstance().getLastError(m_iPort));
                    return;
                }
                Log.i(TAG, "getPort succ with: " + m_iPort+"----iDataSize:  "+iDataSize);
                if (iDataSize > 0) {
                    if (!Player.getInstance().setStreamOpenMode(m_iPort, iStreamMode))  //set stream mode
                    {
                        Log.e(TAG, "setStreamOpenMode failed");
                        return;
                    }
                    if (!Player.getInstance().openStream(m_iPort, pDataBuffer, iDataSize, 2*1024*1024)) //open stream
                    {
                        Log.e(TAG, "openStream failed");
                        return;
                    }
                    if(!Player.getInstance().playSound(m_iPort))
                    {
                        Log.e(TAG, "playSound failed with error code:" + Player.getInstance().getLastError(m_iPort));
                        return;
                    }
                }
            } else {
                if (!Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize)) {
                    for(int i = 0; i < 4000 && m_iPlaybackID >=0 ; i++)
                    {
                        if (!Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize))
                            Log.e(TAG, "inputData failed with: " + Player.getInstance().getLastError(m_iPort));
                        else
                            break;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();

                        }
                    }
                }
            }
        }

    }

    /**
     * 设备抓图
     * @param logID
     * @param chanNum
     * @return
     */
    public boolean capture(int logID,int chanNum,String fileNameString){
        try{
            // 图片质量
            NET_DVR_JPEGPARA jpeg = new NET_DVR_JPEGPARA();
            //分辨率
            jpeg.wPicSize = 0xff;
            //图片质量系数 0 1 2  0最好
            jpeg.wPicQuality =0;

            // 图片大小
            ByteBuffer jpegBuffer = ByteBuffer.allocate(1024 * 1024);
            String sjpegBuffer = "";
            boolean b = HCNetSDK.getInstance().NET_DVR_CaptureJPEGPicture(logID, chanNum, jpeg, fileNameString);
            Log.e(TAG, "NET_DVR_capture is failed!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return b;
        } catch (Exception err) {
            Log.e(TAG, "error: " + err.toString());
        }
        return false;
    }
}
