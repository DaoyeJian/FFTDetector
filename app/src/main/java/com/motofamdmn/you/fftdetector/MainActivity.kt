//FFT Detector version 2.0.1
//2021.5.24 更新

package com.motofamdmn.you.fftdetector

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "AudioRecordTest"

    private val REQUEST_PERMISSION_ID = 200

    private var permissionToRecordAccepted = false
    private var permissionToWriteExternalStorageAccepted = false

    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE)

    // 録音と外部ストレージ書込みのパーミッション確認
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_ID) {
            // 識別IDでリクエストを判断
            if (grantResults.isNotEmpty()) {
                // 処理された
                for (i in permissions.indices) {
                    // 複数リクエストがあった場合
                    if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        // 外部ストレージのパーミッション
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            permissionToWriteExternalStorageAccepted = true
                        } else {
                            // 拒否
                        }
                    }
                    if (permissions[i] == Manifest.permission.RECORD_AUDIO) {
                        // 録音のパーミッション
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            permissionToRecordAccepted = true
                        } else {
                            // 拒否
                        }
                    }

                }
            }
        }

        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //permissionToRecordAccepted = if (requestCode == REQUEST_PERMISSION_ID) {
        //    grantResults[0] == PackageManager.PERMISSION_GRANTED
        //} else {
        //    false
        //}
        if (!permissionToRecordAccepted) finish()
    }

    //オプションメニューの表示
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //オプションメニューが選択された時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.termsOfUseItem -> {  //利用規約
                AlertDialog.Builder(this)
                    .setView(R.layout.terms_of_use)
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        // Do nothing
                    }
                    .show()
                true
            }
            R.id.privacyPolicyItem -> {  //プライバシーポリシー
                AlertDialog.Builder(this)
                    .setView(R.layout.oss_license)
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        // Do nothing
                    }
                    .show()
                true
            }
            R.id.versionInfoItem -> {  //バージョン情報
                AlertDialog.Builder(this)
                    .setView(R.layout.version_info)
                    .setPositiveButton(
                        "OK"
                    ) { _, _ ->
                        // Do nothing
                    }
                    .show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Otoha)  //splash画面テーマを元のAPPThemeに戻す
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //splash画面
        //setTheme(R.style.AppTheme_Splash)
        setContentView(R.layout.activity_main)

        //`ツールバー（アクションバー）の設定
        toolbar.setTitle("FFT Detector")
        setSupportActionBar(findViewById(R.id.toolbar))
        //toolbar.setLogo(R.drawable.ic_launcher_foreground)

        // 録音のパーミッションリクエスト
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_ID)

        val fragmentManager = this.getSupportFragmentManager()

        //録音フラグメント（recordSound）の生成,tagをrecordFragmentとする
        val fragment = recordSound()
        val fragmentTransaction = fragmentManager.beginTransaction().setTransition(
            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
        )
        fragmentTransaction.add(R.id.container, fragment, "recordSoundFragment").commit()

        // TabLayoutの取得
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        var tPosition = 0

        // OnTabSelectedListenerの実装
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            // タブが選択された際に呼ばれる
            override fun onTabSelected(tab: TabLayout.Tab) {

                val tabPosition = tab.position
                val nowFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.container)
                val rSFg = fragmentManager.findFragmentByTag("recordSoundFragment")
                val fAFg = fragmentManager.findFragmentByTag("fftAnalisysFragment")
                val sCFg = fragmentManager.findFragmentByTag("setConditionFragment")

                when (tabPosition) {
                    0 -> {
                        val rFragment = recordSound()
                        val mfragmentTransaction = fragmentManager.beginTransaction().setTransition(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                        )
                        if (fAFg != null) {
                            mfragmentTransaction.remove(fAFg)
                            mfragmentTransaction.hide(fAFg)
                        }
                        if (sCFg != null && tPosition == 2) {
                            mfragmentTransaction.hide(sCFg)
                        }
                        if (rSFg != null) {
                            mfragmentTransaction.show(rSFg)
                        } else {
                            mfragmentTransaction.add(
                                R.id.container,
                                rFragment,
                                "recordSoundFragment"
                            )
                        }
                        mfragmentTransaction.commit()
                        tPosition = 0
                    }

                    1 -> {
                        val fFragment = fftAnalisys()
                        val mfragmentTransaction = fragmentManager.beginTransaction().setTransition(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                        )

                        if (rSFg != null && tPosition == 0) {
                            mfragmentTransaction.hide(rSFg)
                        }
                        if (sCFg != null && tPosition == 2) {
                            mfragmentTransaction.hide(sCFg)
                        }
                        if (fAFg != null) {
                            mfragmentTransaction.remove(fAFg)
                        } else {
                            mfragmentTransaction.add(
                                R.id.container,
                                fFragment,
                                "fftAnalisysFragment"
                            )
                        }
                        mfragmentTransaction.commit()
                        tPosition = 1

                    }

                    else -> {
                        val sFragment = setCondition()
                        val mfragmentTransaction = fragmentManager.beginTransaction().setTransition(
                            FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                        )

                        if (rSFg != null && tPosition == 0) {
                            mfragmentTransaction.hide(rSFg)
                        }
                        if (fAFg != null) {
                            mfragmentTransaction.remove(fAFg)
                            //mfragmentTransaction.hide(fAFg)
                        }
                        if (sCFg != null) {
                            mfragmentTransaction.show(sCFg)
                        } else {
                            mfragmentTransaction.add(
                                R.id.container,
                                sFragment,
                                "setConditionFragment"
                            )
                        }
                        mfragmentTransaction.commit()
                        tPosition = 2

                    }
                }

            }

            // タブが未選択になった際に呼ばれる
            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            // 同じタブが選択された際に呼ばれる
            override fun onTabReselected(tab: TabLayout.Tab) {

            }


        })

    }


}