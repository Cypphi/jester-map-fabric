plugins {
	id("dev.kikugie.stonecutter")
}

stonecutter.active("26.1.2") /* [SC] DO NOT EDIT */

stonecutter {
	parameters {
		constants["jester_mc_262_or_newer"] = current.parsed >= "26.2"
	}
}
