import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.ui.contextmenu.ContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider
import com.nickcoblentz.montoya.LogLevel
import com.nickcoblentz.montoya.MontoyaLogger
import kotlinx.coroutines.*
import java.awt.Component
import java.util.concurrent.Executors
import javax.swing.JMenuItem
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


// Montoya API Documentation: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html
// Montoya Extension Examples: https://github.com/PortSwigger/burp-extensions-montoya-api-examples

class ApplyAnomalyRank : BurpExtension, ContextMenuItemsProvider {
    private val requestResponses = mutableListOf<HttpRequestResponse>()
    private lateinit var api: MontoyaApi
    private lateinit var logger: MontoyaLogger
    private val applyAnomalyRankMenuItem = JMenuItem("Apply Anomaly Rank")
    private val customAnomalyRankScope = CoroutineScope(Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher())

    //private val projectSettings : MyProjectSettings by lazy { MyProjectSettings() }

    companion object {
        const val EXTENSION_NAME = "Apply Anomaly Rank"
    }




    override fun initialize(api: MontoyaApi?) {

        // In Kotlin, you have to explicitly define variables as nullable with a ? as in MontoyaApi? above
        // This is necessary because the Java Library allows null to be passed into this function
        // requireNotNull is a built-in Kotlin function to check for null that throws an Illegal Argument exception if it is null
        // after checking for null, the Kotlin compiler knows that any reference to api  or this.api below will not = null and you no longer have to check it
        // Finally, assign the MontoyaApi instance (not nullable) to a class property to be accessible from other functions in this class
        this.api = requireNotNull(api) { "api : MontoyaApi is not allowed to be null" }
        // This will print to Burp Suite's Extension output and can be used to debug whether the extension loaded properly
        logger = MontoyaLogger(api, LogLevel.DEBUG)
        logger.debugLog("Started loading the extension...")


        // Name our extension when it is displayed inside of Burp Suite
        api.extension().setName(EXTENSION_NAME)

        // Code for setting up your extension starts here...

        applyAnomalyRankMenuItem.addActionListener {
            e -> applyAnomalyRank()
        }

        api.userInterface().registerContextMenuItemsProvider(this)

        api.extension().registerUnloadingHandler {
            customAnomalyRankScope.cancel()
        }

        // Just a simple hello world to start with


        // Code for setting up your extension ends here
        //api.userInterface().registerSettingsPanel(projectSettings.settingsPanel)

        // See logging comment above
        logger.debugLog("...Finished loading the extension")

    }

    @OptIn(ExperimentalTime::class)
    private fun applyAnomalyRank() {
        customAnomalyRankScope.launch {
            val rankedRequests = api.utilities().rankingUtils().rank(requestResponses)
            val timestamp = Clock.System.now().epochSeconds
            val maxRank = rankedRequests.maxOf { it.rank() }
            val maxLength = maxRank.toString().length

            for (i in requestResponses.indices) {
                //if (!isActive) break
                ensureActive()

                if (i < rankedRequests.size) {
                    val floatRank = rankedRequests[i].rank()


                    requestResponses[i].annotations()
                        .setNotes("Anom Rank $timestamp: ${String.format("%0${maxLength}d", floatRank)}")
                }
            }
        }
    }

    override fun provideMenuItems(event: ContextMenuEvent?): List<Component?> {
        event?.let { nonNullEvent ->
            event.selectedRequestResponses().let { selectedRequestResponse ->
                requestResponses.clear()
                requestResponses.addAll(selectedRequestResponse)
                return listOf(applyAnomalyRankMenuItem)
            }
        }

        return emptyList()
    }
}


//class MyProjectSettings() {
//    val settingsPanelBuilder : SettingsPanelBuilder = SettingsPanelBuilder.settingsPanel()
//        .withPersistence(SettingsPanelPersistence.PROJECT_SETTINGS) // you can change this to user settings if you wish
//        .withTitle(YourBurpKotlinExtensionName.EXTENSION_NAME)
//        .withDescription("Add your description here")
//        .withKeywords("Add Keywords","Here")
//
//    private val settingsManager = PanelSettingsDelegate(settingsPanelBuilder)
//
//    val example1Setting: String by settingsManager.stringSetting("An example string setting here", "test default value here")
//    val example2Setting: Boolean by settingsManager.booleanSetting("An example boolean setting here", false)
//
//    val settingsPanel = settingsManager.buildSettingsPanel()
//}