package kr.ac.snu.hcil.omnitrack

import android.app.Application
import android.graphics.Color
import android.os.AsyncTask
import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.core.*
import kr.ac.snu.hcil.omnitrack.core.attributes.*
import kr.ac.snu.hcil.omnitrack.core.database.CacheHelper
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseHelper
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitRecentSleepTimeMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitStepCountMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitStepsFactory
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitStepMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerManager
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTApplication : Application() {

    companion object {
        lateinit var app: OTApplication
            private set

        const val INTENT_EXTRA_OBJECT_ID_TRACKER = "trackerObjectId"
        const val INTENT_EXTRA_OBJECT_ID_ATTRIBUTE = "attributeObjectId"
        const val INTENT_EXTRA_OBJECT_ID_USER = "userObjectId"
        const val INTENT_EXTRA_OBJECT_ID_TRIGGER = "triggerObjectId"
        const val INTENT_EXTRA_DB_ID_ITEM = "itemDbId"


        const val INTENT_EXTRA_ITEMBUILDER = "itemBuilderId"

        const val BROADCAST_ACTION_TIME_TRIGGER_ALARM = "kr.ac.snu.hcil.omnitrack.action.ALARM"
        const val BROADCAST_ACTION_EVENT_TRIGGER_CHECK_ALARM = "kr.ac.snu.hcil.omnitrack.action.EVENT_TRIGGER_ALARM"


        /*
        const val BROADCAST_ACTION_SHORTCUT_PUSH_NOW = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_PUSH_NOW"
        const val BROADCAST_ACTION_SHORTCUT_OPEN_TRACKER = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_OPEN_TRACKER"
        const val BROADCAST_ACTION_SHORTCUT_INCLUDE_TRACKER = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_INCLUDE_TRACKER"
        const val BROADCAST_ACTION_SHORTCUT_EXCLUDE_TRACKER = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_EXCLUDE_TRACKER"
        const val BROADCAST_ACTION_SHORTCUT_TRACKER_INFO_CHANGED = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_TRACKER_INFO_CHANGED"
*/
        const val BROADCAST_ACTION_SHORTCUT_REFRESH = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_TRACKER_REFRESH"

        const val BROADCAST_ACTION_ITEM_ADDED = "kr.ac.snu.hcil.omnitrack.action.ITEM_ADDED"
        const val BROADCAST_ACTION_ITEM_REMOVED = "kr.ac.snu.hcil.omnitrack.action.ITEM_REMOVED"
        const val BROADCAST_ACTION_ITEM_EDITED = "kr.ac.snu.hcil.omnitrack.action.ITEM_EDITED"

        const val BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED = "kr.ac.snu.hcil.omnitrack.action.BACKGROUND_LOGGING_STARTED"
        const val BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED = "kr.ac.snu.hcil.omnitrack.action.BACKGROUND_LOGGING_SUCCEEDED"

        const val PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_foreground"

        const val PREFERENCE_KEY_BACKGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_background"
    }

    private lateinit var _currentUser: OTUser

    lateinit var dbHelper: DatabaseHelper
        private set

    val cacheHelper: CacheHelper by lazy {
        CacheHelper(this)
    }

    val currentUser: OTUser
        get() {
            return _currentUser
        }

    val colorPalette: IntArray by lazy {
        this.resources.getStringArray(R.array.colorPaletteArray).map { Color.parseColor(it) }.toIntArray()
    }

    val googleApiKey: String by lazy {
        this.resources.getString(R.string.google_maps_key)
    }

    lateinit var triggerManager: OTTriggerManager
        private set


    lateinit var supportedAttributePresets: Array<AttributePresetInfo>
        private set

    override fun onCreate() {
        super.onCreate()

        app = this

        dbHelper = DatabaseHelper(this)

        var initialRun = false
        val user = dbHelper.findUserById(1)
        if (user == null) {
            val defaultUser = OTUser("Young-Ho Kim", "yhkim@hcil.snu.ac.kr")
            _currentUser = defaultUser

            initialRun = true

        } else {
            _currentUser = user
        }


        triggerManager = OTTriggerManager(_currentUser, if (_currentUser.dbId != null) {
            dbHelper.findTriggersOfUser(_currentUser.dbId!!)
        } else {
            null
        })


        for (service in OTExternalService.availableServices) {
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                service.prepareServiceAsync({
                    result ->

                })
            }
        }

        for (tracker in currentUser.getTrackersOnShortcut()) {
            OTShortcutManager += tracker
        }


        supportedAttributePresets = arrayOf(
                SimpleAttributePresetInfo(OTAttribute.TYPE_SHORT_TEXT, R.drawable.field_icon_shorttext, this.getString(R.string.type_shorttext_name), this.getString(R.string.type_shorttext_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_LONG_TEXT, R.drawable.field_icon_longtext, this.getString(R.string.type_longtext_name), this.getString(R.string.type_longtext_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_NUMBER, R.drawable.field_icon_number, this.getString(R.string.type_number_name), this.getString(R.string.type_number_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_RATING, R.drawable.field_icon_rating, this.getString(R.string.type_rating_name), this.getString(R.string.type_rating_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time, this.getString(R.string.type_timepoint_name), this.getString(R.string.type_timepoint_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_TIMESPAN, R.drawable.field_icon_timer, this.getString(R.string.type_timespan_name), this.getString(R.string.type_timespan_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_LOCATION, R.drawable.field_icon_location, this.getString(R.string.type_location_name), this.getString(R.string.type_location_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_IMAGE, R.drawable.field_icon_image, this.getString(R.string.type_image_name), this.getString(R.string.type_image_desc)),

                AttributePresetInfo(R.drawable.field_icon_singlechoice, this.getString(R.string.type_single_choice_name), this.getString(R.string.type_single_choice_desc),
                        { user, columnName ->
                            val attr = OTAttribute.createAttribute(user, columnName, OTAttribute.TYPE_CHOICE) as OTChoiceAttribute
                            attr.allowedMultiSelection = false
                            attr
                        }),

                AttributePresetInfo(R.drawable.field_icon_multiplechoice, this.getString(R.string.type_multiple_choices_name), this.getString(R.string.type_multiple_choices_desc),
                        { user, columnName ->
                            val attr = OTAttribute.createAttribute(user, columnName, OTAttribute.TYPE_CHOICE) as OTChoiceAttribute
                            attr.allowedMultiSelection = true
                            attr
                        })

        )

        if (initialRun) {
            AsyncTask.execute {
                createExampleTrackers()
            }
        }
    }

    fun syncUserToDb() {
        dbHelper.save(_currentUser)
        for (triggerEntry in triggerManager.withIndex()) {
            dbHelper.save(triggerEntry.value, _currentUser, triggerEntry.index)
        }
        dbHelper.deleteObjects(DatabaseHelper.TriggerScheme, *triggerManager.fetchRemovedTriggerIds())

        OTTimeTriggerAlarmManager.storeTableToPreferences()
    }

    override fun onTerminate() {
        super.onTerminate()

        syncUserToDb()

        dbHelper.close()
    }

    private fun createExampleTrackers() {
        //====================================================================================================================================================
        val coffeeTracker = currentUser.newTracker("Coffee", true)
        coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Name", OTAttribute.TYPE_SHORT_TEXT))
        coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Drank At", OTAttribute.TYPE_TIME))

        val waterTracker = currentUser.newTracker("Water", true)
        waterTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Drank At", OTAttribute.TYPE_TIME))


        //====================================================================================================================================================
        val sleepTracker = currentUser.newTracker("Sleep", true)

        val sleepTimeAttribute = OTAttribute.Companion.createAttribute(currentUser, "Sleep Duration", OTAttribute.TYPE_TIMESPAN)
        sleepTimeAttribute.setPropertyValue(OTTimeSpanAttribute.PROPERTY_GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)

        val sleepTimeConnection = OTConnection()
        sleepTimeConnection.source = FitbitRecentSleepTimeMeasureFactory.makeMeasure()
        sleepTimeConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, 0)
        sleepTimeAttribute.valueConnection = sleepTimeConnection

        sleepTracker.attributes.add(sleepTimeAttribute)
        sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Quality", OTAttribute.TYPE_RATING))
        sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Memo", OTAttribute.TYPE_LONG_TEXT))

        //====================================================================================================================================================
        val beerTracker = currentUser.newTracker("Beer", true)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Photo", OTAttribute.TYPE_IMAGE))
        val dateAttribute = OTAttribute.createAttribute(currentUser, "Date", OTAttribute.TYPE_TIME)
        dateAttribute.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        beerTracker.attributes.add(dateAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Name", OTAttribute.TYPE_SHORT_TEXT))

        val typeAttribute = OTAttribute.createAttribute(currentUser, "Type", OTAttribute.TYPE_CHOICE)
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES, UniqueStringEntryList("Lager", "Stout", "Ale", "Hybrid"))
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        beerTracker.attributes.add(typeAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Score", OTAttribute.TYPE_RATING))
        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Review", OTAttribute.TYPE_LONG_TEXT))


        //====================================================================================================================================================
        val stepComparisonTracker = currentUser.newTracker("Step Devices", true)
        val fitbitAttribute = OTAttribute.createAttribute(currentUser, "Fitbit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        fitbitAttribute.numberStyle.fractionPart = 0
        val fitbitStepConnection = OTConnection()
        fitbitStepConnection.source = FitbitStepCountMeasureFactory.makeMeasure()
        fitbitStepConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        fitbitAttribute.valueConnection = fitbitStepConnection

        val misfitAttribute = OTAttribute.createAttribute(currentUser, "MisFit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        misfitAttribute.numberStyle.fractionPart = 0
        val misfitStepConnection = OTConnection()

        misfitStepConnection.source = MisfitStepMeasureFactory.makeMeasure()
        misfitStepConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        misfitAttribute.valueConnection = misfitStepConnection

        val googleFitAttribute = OTAttribute.createAttribute(currentUser, "Google Fit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        googleFitAttribute.numberStyle.fractionPart = 0
        val googleFitConnection = OTConnection()
        googleFitConnection.source = GoogleFitStepsFactory.makeMeasure()
        googleFitConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        googleFitAttribute.valueConnection = googleFitConnection

        stepComparisonTracker.attributes.add(fitbitAttribute)
        stepComparisonTracker.attributes.add(misfitAttribute)
        stepComparisonTracker.attributes.add(googleFitAttribute)

        dbHelper.save(_currentUser)

        java.lang.Runnable {

            //batch input
            val end = System.currentTimeMillis()
            val start = end - 200 * DateUtils.DAY_IN_MILLIS


            val sleepItems = ArrayList<OTItem>()
            val coffeeItems = ArrayList<OTItem>()


            TimeHelper.loopForDays(start, end)
            {
                time, from, to, dayOfYear ->

                val wakeUp = from + 8 * DateUtils.HOUR_IN_MILLIS + (Math.random() * DateUtils.HOUR_IN_MILLIS * 1.5).toLong()
                sleepItems.add(
                        OTItem(
                                sleepTracker,
                                wakeUp + (Math.random() * DateUtils.HOUR_IN_MILLIS).toLong(),
                                TimeSpan.fromPoints(from + (Math.random() * 3 * DateUtils.HOUR_IN_MILLIS - 1.5 * DateUtils.HOUR_IN_MILLIS).toLong(), wakeUp),
                                (Math.random() * 5).toFloat(),
                                ""
                        )
                )

                val numCoffeeADay = ((Math.random() * 4) + .5f).toInt()
                for (i in 0..numCoffeeADay - 1) {
                    val coffeeTime = from + ((to - from) * Math.random()).toLong()
                    coffeeItems.add(
                            OTItem(
                                    coffeeTracker,
                                    coffeeTime,
                                    "Americano",
                                    coffeeTime
                            )
                    )
                }
            }

            addStepsExampleItems(stepComparisonTracker)

            addExampleBeerReviews(beerTracker)

            dbHelper.save(sleepItems, sleepTracker)
            dbHelper.save(coffeeItems, coffeeTracker)


        }.run()
    }

    private fun addStepsExampleItems(stepTracker: OTTracker) {

        //actual data:  9/10 ~ 9/18 fake data: 9/2 ~ 9/10
        val fitbitSteps = arrayOf(11200 as Any?, 14246, 11700, 13820, 12500, 10270, 15000, 9416, 3665, 6384, 22013, 13709, 10096, 13611, 16265, 8187, 6045)

        //9/2 ~ 9/18
        val misfitSteps = arrayOf(10506 as Any?, 12700, 10464, 12576, 10748, 9248, 12478, 7416, 10776, 7414, 14000, 11298, 7762, 11558, 13810, 4210, 5198)

        //9/2 ~ 9/18
        val googleSteps = arrayOf(5623 as Any?, 8086, 5806, 5913, 0, 1994, 5108, 502, 5024, 2518, 3654, 5302, 2054, 3220, 5394, 0, 436)

        val stepStart = GregorianCalendar(2016, 8, 3).timeInMillis

        val timestamps = LongArray(18 - 2 + 1) {
            index ->
            stepStart + index * DateUtils.DAY_IN_MILLIS
        }

        val stepItems = ArrayList<OTItem>()
        OTItem.createItemsWithColumnArrays(stepTracker, timestamps, stepItems, fitbitSteps, misfitSteps, googleSteps)

        dbHelper.save(stepItems, stepTracker)
    }

    private fun addExampleBeerReviews(beerTracker: OTTracker) {
        val rowValues = arrayOf(
                arrayOf("https://images.vingle.net/upload/t_ca_xl/jop8mlrsqtahbt8uuyzr.jpg", GregorianCalendar(2016, 7, 22, 20, 30).timeInMillis, "Green Flash West Coast IPA", intArrayOf(2), 3f, "ATL에서 수입을 재개한 그린플래쉬 양조장의 IIPA입니다. 2014년 이전 버전 West Coast IPA(뭉뚝한 병)은 IPA였지만, 2014년 이후 그린플래쉬 라인업 리뉴얼 이후 임페리얼 IPA가 되어서 돌아왔네요. 여타 다른 그린플래쉬 맥주들이 2014년 이전 버전이 더 낫다는 평을 듣고있고, 이 맥주 또한 그렇습니다. 당연히 IPA인줄 알고 마셨는데 지잉하는 홉향에 놀라서 병을 보니 더블 IPA라고 써있더라고요. 흔한 IIPA에서 볼수있는 오렌지와 호박색 중간쯤 되는 색을 띄고 있고, 홉향은 허브향에 가깝습니다. 맥아의 존재감 역시 더블 IPA인 만큼 홉향에 가려져서 거의 느낄 수 없습니다. 전체적으로 무난한 IIPA고 마시기 좋습니다."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/hv9buv5fixwtjlw2qmdp.jpg", GregorianCalendar(2016, 7, 22, 17, 30).timeInMillis, "Cuvee Delphine", intArrayOf(1), 4.5f, "어떻게 이런 맛을 뽑아낼 수 있는지요. 세번째 스트루이즈 양조장의 임페리얼 스타우트 쿠베 델핀입니다. 라벨에는 \"진리가 너희를 자유케 하리라\" 라는 요한복음의 구절이 써있네요. 벨기에의 아티스트인 Delphine Boël이 그린 라벨이고, 아티스트의 이름을 따서 맥주의 이름도 쿠베 Delphine입니다."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/wkgjgf7hswzpaousvjhn.jpg", GregorianCalendar(2016, 7, 20, 21, 30).timeInMillis, "Struise Pannepot", intArrayOf(2), 3f, "Struise 양조장의 쿼드루펠, 팬네폿입니다. 얼마만에 마셔보는 완벽한 밸런스의 쿼드루펠인지요. 아무것도 부족하지 않습니다. 약간의 점성이 느껴지는 짙은 갈색의 맥주를 따르면, 아주 조금의 갈색의 거품이 잔 위를 덮습니다. 깊숙한 카라멜 맥아향과 그 위로 깔리는 말린 검은과일향."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/pmaf21cycyjiieisog0m.jpg", GregorianCalendar(2016, 7, 18, 20, 30).timeInMillis, "Verboden Vrucht", intArrayOf(2), 4f, "스트롱 다크 에일이지만 굴덴 드락이나 쿼드루펠들 같은 이스트향은 느껴지지 않습니다. 다만 어디서 나는지 모를 단 향(.. 약간 한약재 비스무리한..?)이 잠깐 나다가 툭 끊기는데에서 어쩐지 금단의 열매라는 이름이 와닿습니다."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/p95ybbv38ufnncqncgix.jpg", GregorianCalendar(2016, 7, 15, 19, 30).timeInMillis, "Ypres Reserva", intArrayOf(3), 3.5f, " 첫번째로 마신 맥주는 Ypres입니다. 벨기에의 도시 이퍼르를 따온건데, 세계 1차대전 당시의 짤막한 일화가 라벨 위에 적혀있습니다. 예전의 Ypers는 이미 Retired 되었다고 하고, 이 맥주는 2011년의 맥주를 숙성/보관한건지 Reserva 2011이라고 써있군요. 여태까지 사워 에일은 빨간색 아니면 노란색만 봐와서 깜장색의 사워 에일을 보는 것은 처음입니다"),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/bjlmhbnix86ixvtgna0j.jpg", GregorianCalendar(2016, 7, 13, 21, 30).timeInMillis, "Hop Head Red", intArrayOf(2), 3.5f, "보통 홉통기한이라고 해서 병입 후 1년 정도가 지나면 홉의 향이 다 날아가서 맛이 없어진다고 하는데 제가 지금 마시고 있는 홉 헤드 레드는 아직도 어마무지한 홉 향을 품고 있습니다. 콜럼버스, 너겟, 아마릴로 홉의 향이 카라멜 향 위에서 날아다니고 있습니다. 색은 브라운에 가까운 호박색이고, 꽤나 씁니다."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/hv9buv5fixwtjlw2qmdp.jpg", GregorianCalendar(2016, 7, 12, 17, 30).timeInMillis, "Orange Avenue Wit", intArrayOf(3), 2.5f, "KGB등 기성 보드카 베이스 칵테일(?)과 견주어도 꿀리지 않을 만큼의 강한 오렌지 향을 가지고 있어서 쉽게쉽게 먹기에 아주 좋은 맥주인 것 같습니다. 다시 말하면, 그 일차원적인 오렌지 향과 맛이 맥주 본연의 말트향과 홉향을 전부 가리고 있다고 할 수도 있겠네요."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/gikqvib13avzk6yh0fo5.jpg", GregorianCalendar(2016, 7, 10, 23, 30).timeInMillis, "By Udder Means", intArrayOf(1), 3f, "미션의 다크 씨와 같은 검은색에 갈색 헤드를 가진 맥주입니다. Udder는 소의 젖통이라는 뜻이죠. 실제로 락토오스(젖당)가 들어간 맥주입니다. 하지만 우유와 같은 맛은 전혀 아니고, 단지 마시고 난 뒤의 바디감이 우유와 흡사했습니다. 마셨을 때의 느낌은 달달한 오트밀 스타우트를 마시는 느낌이었습니다. 약간의 신맛이 있는데, 초콜릿, 커피와 같은 스타우트 본연의 몰트향과 어울리지 못하고 따로 놀아서 약간 안타까웠습니다."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/wldibexysoy03hdtq2q6.jpg", GregorianCalendar(2016, 7, 8, 12, 30).timeInMillis, "Tsingtao", intArrayOf(0), 3f, " 이런 부산물 라거는 바디감이 매우 가볍고, 식용유에 가까운 창백한(pale)색이며 탄산음료를 마시는 것 같은 청량감을 가지고 있습니다. 그렇게 쓰지도 않고, 맥아향도 엷고, 알콜도 5도 아래로 낮죠. 여러모로 대형 양조장에서 수익을 얻기 위해 질을 버리고 양을 취한 맥주입니다.")

        )


        dbHelper.save(
                rowValues.map {
                    OTItem(
                            beerTracker,
                            it[1] as Long,
                            *it
                    )
                }, beerTracker)


    }
}