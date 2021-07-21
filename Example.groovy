package com.kytheralabs.playground

import com.kytheralabs.playground.Select

def map = ["content_section_findadrug_drugComponentContainerLeft_xmlfilter_filtergroup-state": "Hawaii", county: "All", plan: "SilverScript Choice (PDP) - S5601-066"]

val s = new Select();

map.each{entry ->
    s.setElement(entry.value)
    s.selectByVisibleText(entry.value)
}
