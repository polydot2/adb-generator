package com.poly.devtop

import org.eclipse.jgit.api.Git
import java.io.File

object GitIntereactor {
    // Crée le nom de la branche (ex. "feature/ABC-123-description")
    fun createBranchName(ticket: String): String {
        val regex = "^[A-Z]+-[0-9]+$".toRegex()
        if (!ticket.matches(regex)) {
            throw IllegalArgumentException("Ticket JIRA invalide : doit être au format ABC-123")
        }
        return "feature/$ticket-description" // Ajuste la description si nécessaire
    }


    // Crée une branche Git dans le dépôt spécifié
    fun createGitBranch(repoDir: File, branchName: String) {
        val git = Git.open(repoDir)
        try {
            // Vérifier si la branche existe déjà
            val branchExists = git.branchList().call().any { it.name == "refs/heads/$branchName" }
            if (branchExists) {
                throw IllegalStateException("La branche '$branchName' existe déjà")
            }

            // Créer la branche
            git.branchCreate()
                .setName(branchName)
                .call()

            // (Optionnel) Pousser la branche vers l'origine
            // git.push().setRefSpecs("refs/heads/$branchName").call()
        } finally {
            git.close()
        }
    }
}