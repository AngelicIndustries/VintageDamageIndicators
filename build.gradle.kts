
import net.darkhax.curseforgegradle.TaskPublishCurseForge

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

apply(from = "https://raw.githubusercontent.com/JackOfNoneTrades/67minecraft-gradle-publish/master/67minecraft-publish.gradle.kts")

tasks.named<TaskPublishCurseForge>("publishCurseforge") {
    uploadArtifacts.forEach { it.addEnvironment("Client", "Server") }
}
