package com.humanid.humanidui.presentation.phonenumber

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.humanid.auth.HumanIDAuth
import com.humanid.humanidui.R
import com.humanid.humanidui.R.string
import com.humanid.humanidui.event.CloseAllActivityEvent
import com.humanid.humanidui.presentation.HumanIDOptions
import com.humanid.humanidui.presentation.otp.OtpFragment
import com.humanid.humanidui.presentation.otp.OtpFragment.OnVerifyOtpListener
import com.humanid.humanidui.util.PassiveFormFragment
import com.humanid.humanidui.util.emptyString
import com.humanid.humanidui.util.extensions.gone
import com.humanid.humanidui.util.extensions.onClick
import com.humanid.humanidui.util.extensions.showToast
import com.humanid.humanidui.util.extensions.visible
import com.humanid.humanidui.util.makeLinks
import com.humanid.humanidui.util.validation.Validation
import com.humanid.humanidui.util.validation.util.minMaxLengthRule
import com.humanid.humanidui.util.validation.util.notEmptyRule
import com.humanid.humanidui.util.validation.util.numberOnlyRule
import com.humanid.humanidui.util.validation.util.onTextChange
import kotlinx.android.synthetic.main.fragment_phone_number.btnCancel
import kotlinx.android.synthetic.main.fragment_phone_number.btnEnter
import kotlinx.android.synthetic.main.fragment_phone_number.btnTransfer
import kotlinx.android.synthetic.main.fragment_phone_number.ccpPhoneNumber
import kotlinx.android.synthetic.main.fragment_phone_number.edtPhoneNumber
import kotlinx.android.synthetic.main.fragment_phone_number.imgAppLogo
import kotlinx.android.synthetic.main.fragment_phone_number.tvMessage
import kotlinx.android.synthetic.main.fragment_phone_number.tvOTP
import kotlinx.android.synthetic.main.layout_bottom_sheet.bottomSheet
import org.greenrobot.eventbus.EventBus

class PhoneNumberFragment : PassiveFormFragment(), OnVerifyOtpListener {

    private var listener: OnPhoneNumberListener? = null

    private var countryCode: String = emptyString()

    private val TAG = "PhoneNumber"

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    companion object {
        fun newInstance(phoneNumberListener: OnPhoneNumberListener): PhoneNumberFragment {
            val fragment: PhoneNumberFragment = PhoneNumberFragment()
            fragment.listener = phoneNumberListener

            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    override val layoutResource: Int = R.layout.fragment_phone_number

    override fun initIntent() {}

    override fun initUI() {
        context?.let {
            initView()
            setSpannableString()
        }

        configureOtpBottomSheet()

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        showKeyboard(edtPhoneNumber, requireContext())
        edtPhoneNumber.requestFocus()
        edtPhoneNumber.error = null
    }

    private fun configureOtpBottomSheet(){
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheetBehavior.peekHeight = 340

        bottomSheetBehavior.isHideable = false

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    override fun initAction() {
        btnCancel.onClick {
            listener?.onButtonCancelClicked()
        }

        btnTransfer.onClick {
            listener?.onButtonTransferClicked()
        }

        btnEnter.onClick {
            setupFormValidation()
            validate()
        }

        val typface = context?.let { ResourcesCompat.getFont(it, R.font.roboto_bold) }
        ccpPhoneNumber.setTypeFace(typface)

        ccpPhoneNumber.setOnCountryChangeListener {
            countryCode = ccpPhoneNumber.selectedCountryCode
        }

        edtPhoneNumber.onTextChange { text ->
            text.let {
                if (it.startsWith("0")) {
                    if (it.isNotEmpty()) {
                        edtPhoneNumber.setText(it.substring(1))
                    } else {
                        edtPhoneNumber.setText("")
                    }
                }
            }
        }
    }

    private fun enableViews(isEnabled: Boolean) {
        btnEnter.text = if (isEnabled) getString(string.action_enter) else getString(string.action_processing)
        btnEnter.isEnabled = isEnabled
        edtPhoneNumber.isEnabled = isEnabled
    }

    private fun requestOtp() {
        enableViews(false)
        val phoneNumber: String = edtPhoneNumber.text.toString().trim()
        countryCode = ccpPhoneNumber.selectedCountryCode
        val task = HumanIDAuth.getInstance().requestOTP(countryCode, phoneNumber)
        task.addOnCompleteListener {
            if (it.isSuccessful) {
                enableViews(true)
                showOtpFragment(countryCode, phoneNumber, this)
            } else {
                enableViews(true)
                showToast(getString(string.error_message_request_otp_failed))
            }
        }
        task.addOnFailureListener {
            enableViews(true)
            Log.d(TAG, it.message.toString())
            showToast(getString(string.error_message_unable_to_request_otp))
        }
    }

    override fun initProcess() {
        context?.let {
            val humanIDOptions = HumanIDOptions.fromResource(it)
            if (humanIDOptions?.applicationIcon != -1) {
                humanIDOptions?.applicationIcon?.let { it1 ->
                    //imgAppLogo.visible()
                    imgAppLogo.setImageDrawable(ContextCompat.getDrawable(it, it1))
                }
            }

            if (!humanIDOptions?.applicationName.isNullOrEmpty()) {
                tvMessage.text = Html.fromHtml(String.format(getString(string.label_verify_your_phone_number), humanIDOptions?.applicationName))
            }
        }
    }

    override fun setupFormValidation() {
        addValidation(
            Validation(
                edtPhoneNumber,
                listOf(
                    notEmptyRule(getString(string.error_field_required)),
                    numberOnlyRule(getString(string.error_number_format)),
                    minMaxLengthRule(getString(string.error_invalid_phonenumber), 8, 13)
                )
            )
        )
    }

    override fun onValidationFailed() {

    }

    override fun onValidationSuccess() {
        requestOtp()
    }

    private fun initView() {
        tvMessage.visible()
        btnTransfer.gone()
    }

    private fun setSpannableString() {
        tvOTP.text = getString(string.message_otp_verification)

        tvOTP.makeLinks(
            Pair(getString(R.string.label_learn_more), View.OnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://human-id.org/#how-we-protect")))
            })
        )
    }

    private fun showOtpFragment(countryCode: String, phoneNumber: String, onVerifyOtpListener: OnVerifyOtpListener){
        bottomSheet.visible()
        val otpFragment = OtpFragment.newInstance(countryCode = countryCode,
            phoneNumber = phoneNumber, onVerifyOtpListener = onVerifyOtpListener)

        childFragmentManager.beginTransaction()
            .replace(R.id.containerBottomSheet, otpFragment).commitAllowingStateLoss()

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onVerifySuccess(exchangeToken: String) {
        EventBus.getDefault().post(CloseAllActivityEvent(exchangeToken, null))

        finishActivity()
    }

    override fun onVerifyFailed(message: String) {
        EventBus.getDefault().post(CloseAllActivityEvent(null, message))

        finishActivity()
    }

    interface OnPhoneNumberListener {
        fun onButtonCancelClicked()
        fun onButtonTransferClicked()
    }
}