package com.nbs.humanidui.presentation.welcome

import android.app.Activity
import android.content.Intent
import com.nbs.humanidui.R
import com.nbs.humanidui.base.BaseActivity
import com.nbs.humanidui.presentation.phonenumber.PhoneNumberActivity

class HumanIDActivity : BaseActivity(), WelcomeFragment.OnWelcomeButtonListener {
    companion object{
        fun start(activity: Activity){
            val intent = Intent(activity, HumanIDActivity::class.java)
            activity.startActivityForResult(intent, 0x200)
        }
    }

    override val layoutResource: Int
        get() = R.layout.activity_humanid

    override fun initLib() {
    }

    override fun initIntent() {
    }

    override fun initUI() {
    }

    override fun initAction() {
    }

    override fun initProcess() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.containerWelcome, WelcomeFragment.newInstance(this))
                .commitAllowingStateLoss()
    }

    override fun onButtonContinueClicked() {
        PhoneNumberActivity.start(this)
    }
}
