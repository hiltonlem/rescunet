package com.ibm.rescunet

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import kotlinx.android.synthetic.main.activity_getting_started.*

class GettingStartedActivity : AppCompatActivity() {

    companion object {
        private const val PAGE_COUNT = 6
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_getting_started)

        view_pager.adapter = MyPageAdapter(supportFragmentManager)

        fab_right.setOnClickListener {
            if (view_pager.currentItem < PAGE_COUNT - 1)
                view_pager.currentItem += 1
            else
                super.onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (view_pager.currentItem == 0)
            super.onBackPressed()
        else
            view_pager.currentItem -= 1
    }

    private inner class MyPageAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getCount() = PAGE_COUNT
        override fun getItem(p0: Int): Fragment {
            return GettingStartedPage().apply { arguments = Bundle().apply {putInt("page", p0)}  }
        }
    }
}
