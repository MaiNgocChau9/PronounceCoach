package com.pronouncecoach.android.scoring

object LevenshteinAligner {

    data class EditOp(val type: String, val i: Int, val j: Int)

    fun distance(a: List<String>, b: List<String>): Int {
        val n = a.size
        val m = b.size
        val dp = Array(n + 1) { IntArray(m + 1) }

        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[n][m]
    }

    fun align(expected: List<String>, heard: List<String>): List<EditOp> {
        val n = expected.size
        val m = heard.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        val back = Array(n + 1) { IntArray(m + 1) }

        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (expected[i - 1] == heard[j - 1]) 0 else 1
                val del = dp[i - 1][j] + 1
                val ins = dp[i][j - 1] + 1
                val sub = dp[i - 1][j - 1] + cost

                when {
                    sub <= del && sub <= ins -> {
                        dp[i][j] = sub
                        back[i][j] = 2 // match or substitution
                    }
                    del <= ins -> {
                        dp[i][j] = del
                        back[i][j] = 1 // deletion from expected
                    }
                    else -> {
                        dp[i][j] = ins
                        back[i][j] = 3 // insertion (extra in heard)
                    }
                }
            }
        }

        // Traceback
        val ops = mutableListOf<EditOp>()
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && back[i][j] == 2 -> {
                    if (expected[i - 1] != heard[j - 1]) {
                        ops.add(EditOp("sub", i - 1, j - 1))
                    }
                    i--; j--
                }
                i > 0 && back[i][j] == 1 -> {
                    ops.add(EditOp("del", i - 1, -1))
                    i--
                }
                j > 0 -> {
                    ops.add(EditOp("ins", -1, j - 1))
                    j--
                }
                else -> break
            }
        }
        return ops.reversed()
    }

    fun errorRate(expected: List<String>, heard: List<String>): Float {
        val dist = distance(expected, heard)
        return if (expected.isEmpty()) 0f else dist.toFloat() / expected.size
    }
}
