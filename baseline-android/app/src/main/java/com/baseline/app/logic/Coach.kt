package com.baseline.app.logic

import com.baseline.app.data.EvidenceItem
import com.baseline.app.data.Grade
import com.baseline.app.data.evidenceLibrary

/**
 * Offline, deterministic coach built on retrieval over the graded evidence
 * library. Used as the fallback when no API key is configured, and as the
 * safety layer in front of the LLM: emergency and dosing questions never
 * reach a remote model.
 */
data class CoachReply(
    val text: String,
    val evidenceIds: List<String> = emptyList(),
    val isEmergency: Boolean = false,
    val isRefusal: Boolean = false,
    val source: String = "offline"
)

object Coach {

    private val emergencyTerms = listOf(
        "chest pain", "crushing chest", "can't breathe", "cannot breathe", "stroke",
        "face drooping", "slurred speech", "suicide", "kill myself", "end my life",
        "testicle lump", "testicular lump", "blood in urine", "coughing blood",
        "sudden vision loss", "severe headache", "unconscious"
    )

    /** Words that appear everywhere and carry no retrieval signal. */
    private val STOPWORDS = setOf(
        "the", "and", "for", "how", "much", "many", "does", "did", "work", "works",
        "should", "what", "when", "why", "with", "have", "has", "can", "you", "your",
        "about", "really", "actually", "need", "needs", "good", "best", "more", "less",
        "are", "was", "were", "will", "would", "there", "this", "that", "from", "get"
    )

    private val dosingTerms = listOf(
        "dose", "dosage", "how much should i take", "mg should i take", "prescribe",
        "prescription", "should i stop taking", "should i take", "what should i take for",
        "diagnose", "do i have", "is it cancer"
    )

    fun isEmergency(q: String): Boolean {
        val l = q.lowercase()
        return emergencyTerms.any { l.contains(it) }
    }

    fun isDosing(q: String): Boolean {
        val l = q.lowercase()
        return dosingTerms.any { l.contains(it) }
    }

    fun emergencyReply(): CoachReply = CoachReply(
        text = "This sounds like it needs a person, not an app. If this is happening now, " +
            "contact your local emergency number or go to an emergency department. " +
            "If you are having thoughts of harming yourself, please contact a crisis line or " +
            "someone you trust right now. Baseline is a wellness tracker and cannot help with this.",
        isEmergency = true,
        source = "safety"
    )

    fun refusalReply(): CoachReply = CoachReply(
        text = "I can't diagnose, dose, or tell you whether to start or stop a medicine — " +
            "that needs a clinician who can examine you and see your history. What I can do is " +
            "show you what the evidence says about the behaviour behind the question, and give you " +
            "something specific to raise with your doctor.",
        isRefusal = true,
        source = "safety"
    )

    /** Simple keyword retrieval with pillar boosting. Deterministic and testable. */
    fun retrieve(question: String, limit: Int = 2): List<EvidenceItem> {
        val q = question.lowercase()
        val tokens = q.split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in STOPWORDS }
            .toSet()
        if (tokens.isEmpty()) return emptyList()

        data class Scored(val item: EvidenceItem, val score: Int)

        val scored = evidenceLibrary.map { item ->
            val haystack = (
                item.title + " " + item.claim + " " + item.summary + " " + item.pillar + " " +
                    item.grade.meaning
                ).lowercase()
            var score = 0
            tokens.forEach { t ->
                if (haystack.contains(t)) score += 2
                if (item.title.lowercase().contains(t)) score += 3
                if (item.pillar.lowercase().contains(t)) score += 2
            }
            Scored(item, score)
        }

        return scored
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<Scored> { it.score }.thenBy { gradeRank(it.item.grade) })
            .take(limit)
            .map { it.item }
    }

    private fun gradeRank(g: Grade): Int = when (g) {
        Grade.A -> 0
        Grade.B -> 1
        Grade.C -> 2
        Grade.D -> 3
    }

    fun offlineAnswer(question: String): CoachReply {
        if (isEmergency(question)) return emergencyReply()
        val hits = retrieve(question)
        if (hits.isEmpty()) {
            return CoachReply(
                text = "Nothing in the library matches that closely. The library covers seven areas: " +
                    "Move, Sleep, Fuel, Mind, Drive, Connect and Myths. Try naming the behaviour — " +
                    "sleep, alcohol, strength training, stress, libido — rather than the symptom.",
                source = "offline"
            )
        }
        val body = buildString {
            appendLine(hits.first().summary)
            appendLine()
            hits.first().let { first ->
                appendLine("Grade ${first.grade.label} — ${first.grade.meaning}.")
                appendLine("Source: ${first.citation}")
                appendLine("What we don't know: ${first.dontKnow}")
            }
            if (hits.size > 1) {
                appendLine()
                append("Related: ")
                append(hits.drop(1).joinToString("; ") { it.title })
                append(".")
            }
        }.trim()
        return CoachReply(text = body, evidenceIds = hits.map { it.id }, source = "offline")
    }

    /** System prompt for the BYOK model: grounded, cited, and allowed to say "I don't know". */
    fun systemPrompt(): String = buildString {
        appendLine("You are the assistant inside Baseline, a personal wellness tracker for one man.")
        appendLine("Rules you must follow:")
        appendLine("1. Never diagnose, never suggest a dose, never tell the user to start or stop a medicine.")
        appendLine("2. Ground every claim in the evidence list below, and name the item you used.")
        appendLine("3. If the evidence does not cover it, say so plainly. Do not fill the gap with plausible advice.")
        appendLine("4. Be direct and dry. No hype, no 'optimise', no 'biohack', no flattery.")
        appendLine("5. End with one concrete action and one thing to measure.")
        appendLine("6. If the user describes an emergency, tell them to contact emergency services. Nothing else.")
        appendLine()
        appendLine("EVIDENCE LIBRARY (grade | title | claim | caveat):")
        evidenceLibrary.forEach {
            appendLine("- [${it.grade.label}] ${it.title}: ${it.claim} Caveat: ${it.dontKnow}")
        }
    }.trim()
}
