
import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

apply(from = "https://raw.githubusercontent.com/AngelicIndustries/67minecraft-gradle-publish/${property("publish67ScriptTag")}/67minecraft-publish.gradle.kts")

tasks.named<TaskPublishCurseForge>("publishCurseforge") {
    uploadArtifacts.forEach { it.addEnvironment("Client", "Server") }
}

extensions.configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Vintage Damage Indicators")
            description.set("Damage indicators, potion status, and damage particles for Minecraft 1.7.10.")
            url.set("https://github.com/AngelicIndustries/VintageDamageIndicators")
            organization {
                name.set("Angelic Industries")
                url.set("https://angelic.industries/")
            }
            scm {
                url.set("https://github.com/AngelicIndustries/VintageDamageIndicators")
                connection.set("scm:git:https://github.com/AngelicIndustries/VintageDamageIndicators.git")
                developerConnection.set("scm:git:ssh://git@github.com/AngelicIndustries/VintageDamageIndicators.git")
            }
        }
    }
}
