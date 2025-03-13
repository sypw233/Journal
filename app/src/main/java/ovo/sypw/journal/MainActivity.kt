package ovo.sypw.journal

import SnackbarUtils
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil3.Bitmap
import ovo.sypw.journal.components.AddItemFAB
import ovo.sypw.journal.components.MainView
import ovo.sypw.journal.components.TopBarView
import ovo.sypw.journal.model.JournalData
import ovo.sypw.journal.ui.theme.JournalTheme
import ovo.sypw.journal.utils.ImageLoadUtils
import java.util.Date

@SuppressLint("RestrictedApi")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JournalTheme {
                ContentViews()

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ContentViews() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    ImageLoadUtils.init(LocalContext.current)
    // 初始化列表状态，并设置初始滚动位置到列表底部
    val cardItems = remember {
        mutableStateListOf<JournalData>().apply {}
    }
    val bitmapList1 = remember {
        mutableListOf<Bitmap>()
    }
    for (i in R.raw.test_image1..R.raw.test_image1 + 0) {
        bitmapList1.add(
            BitmapFactory.decodeResource(
                LocalContext.current.resources, i
            )
        )
    }
    val bitmapList2 = remember {
        mutableListOf<Bitmap>()
    }
    for (i in R.raw.test_image1..R.raw.test_image1 + 1) {
        bitmapList2.add(
            BitmapFactory.decodeResource(
                LocalContext.current.resources, i
            )
        )
    }
    val bitmapList3 = remember {
        mutableListOf<Bitmap>()
    }
    for (i in R.raw.test_image1..R.raw.test_image1 + 2) {
        bitmapList3.add(
            BitmapFactory.decodeResource(
                LocalContext.current.resources, i
            )
        )
    }
    val bitmapList4 = remember {
        mutableListOf<Bitmap>()
    }
    for (i in R.raw.test_image1..R.raw.test_image1 + 3) {
        bitmapList4.add(
            BitmapFactory.decodeResource(
                LocalContext.current.resources, i
            )
        )
    }
    val bitmapList5 = remember {
        mutableListOf<Bitmap>()
    }
    for (i in R.raw.test_image1..R.raw.test_image1 + 4) {
        bitmapList5.add(
            BitmapFactory.decodeResource(
                LocalContext.current.resources, i
            )
        )
    }
    val bitmapList6 = remember {
        mutableListOf<Bitmap>()
    }
    for (i in R.raw.test_image1..R.raw.test_image1 + 5) {
        bitmapList6.add(
            BitmapFactory.decodeResource(
                LocalContext.current.resources, i
            )
        )
    }
    cardItems.add(
        JournalData(
            location = Location("失忆喷雾").apply {
                latitude = 24.630353
                longitude = 118.094141
            },
            date = Date(),
            images = bitmapList1,
            text = "《恋爱猪脚饭》——工地与猪脚饭交织的浪漫邂逅！\n" + "\"当你以为人生已经烂尾时，命运的混凝土搅拌机正在偷偷运转！\"\n" + "破产老哥黄夏揣着最后的房租钱，逃进花都城中村的握手楼。本想和小茂等挂壁老哥一起吃猪脚饭躺平摆烂，却意外邂逅工地女神“陈嘉怡”，从而开启新的土木逆袭人生。\n" + "爽了，干土木的又爽了！即使在底层已经彻底有了的我们，也能通过奋斗拥有美好的明天！"
        )
    )
    cardItems.add(
        JournalData(
            location = Location("土木老哥").apply {
                latitude = 24.630353
                longitude = 118.094141
            },
            images = bitmapList2,
            date = Date(),
            text = "花都七月黏稠的空气里飘着沥青融化的焦味，黄夏攥着塑封袋里最后三张百元钞，在城中村的肠粉摊前数了第五遍。手机还在不停震动，" + "\"金城建筑公司债务清算群\"的红色数字跳成37，像块烧红的铁烙在掌心。"
        )
    )
    cardItems.add(
        JournalData(
            location = Location("土木老哥").apply {
                latitude = 24.630353
                longitude = 118.094141
            },
            images = bitmapList3,
            date = Date(),
            text = "黄夏这辈子做过最贵的决定，是在猪脚饭店用最后300块买了个爱情事故。\n" + "\n" + "当辣椒油泼在陈嘉怡工装裤上的瞬间，他精准计算出这个月要洗多少件文化衫才能抵干洗费。但当他抬头看到对方用电子测距仪测量油渍扩散半径时，突然意识到自己可能误入了某个高科技碰瓷团伙。\n" + "\n" + "\"根据GB/T 3920纺织品色牢度测试标准...\"陈嘉怡推了推银边护目镜，胸前的一级注册结构工程师徽章在霓虹灯下泛着冷光，\"建议你用60℃水温配合过氧化氢溶液...\"\n" + "\n" + "\"停！\"黄夏举起沾着卤汁的筷子投降，\"我赔你整条裤子，只要别让我回忆高考化学。\"\n" + "\n" + "蹲在隔壁扒饭的小茂突然喷出半颗卤蛋：\"陈工可是剑桥回来的混凝土西施，她那套工装顶你半年房租！\"\n" + "\n" + "整个大排档突然响起此起彼伏的咳嗽声，工人们默契地用筷子敲打餐盘，仿佛在庆祝第38个掉进女神陷阱的冤大头。黄夏盯着顺着桌腿蜿蜒的辣椒油，终于明白为什么这里的瓷砖缝都泛着可疑的红色——这分明是处决破产舔狗的行刑场！"
        )
    )
    cardItems.add(
        JournalData(
            images = bitmapList4,
            location = Location("土木老哥").apply {
                latitude = 24.630353
                longitude = 118.094141
            },
            date = Date(),
            text = "暴雨倾盆的午夜，黄夏被警报声惊醒时，正梦见自己变成会说话的混凝土试块。工地探照灯穿透活动板房裂缝，在他脸上切出明暗交织的焦虑光谱。\n" + "\n" + "\"陈工被困在基坑了！\"安全员苏倩的安全帽上水钻乱颤，\"说是支撑架位移超标...\"\n" + "\n" + "黄夏抄起反光背心往外冲，突然被小茂拽住裤腰：\"你个搬砖临时工凑什么热闹？\"\n" + "\n" + "\"我见过那个魔鬼螺栓！\"黄夏眼前闪过三天前的月光，陈嘉怡教他认脚手架型号时，有个银亮螺栓在暗处诡异地反光，\"东南角三号架第十层，就像你喝完假酒乱转的眼珠子！\"\n" + "\n" + "当他连滚带爬冲进塌方区时，正撞见陈嘉怡在泥浆里画受力分析图。女工程师的白色安全帽已经变成奥利奥味，手里的激光笔却稳得像手术刀：\"现在需要重新计算被动区土压力...\"\n" + "\n" + "\"螺栓松了！\"黄夏扑过去抓住她手腕，突然发现对方睫毛上凝着泥水结晶，\"礼拜二晚上十点十七分，三号架第十层西南侧的高强螺栓，扭矩绝对不够！\"\n" + "\n" + "陈嘉怡的瞳孔在暴雨中收缩成猫科动物的竖线：\"你为什么会注意到...\"\n" + "\n" + "\"因为那天你站在月光下...\"黄夏的喉结可疑地滑动，\"像根会发光的钢筋混凝土！\""
        )
    )
    cardItems.add(JournalData(images = bitmapList5, text = "和魏志阳邂逅一场潮汕牛肉的爱情"))
    cardItems.add(JournalData(images = bitmapList6, text = "和魏志阳邂逅999场潮汕牛肉的爱情"))
    for (i in 1..10) {
        cardItems.add(JournalData(text = "和魏志阳邂逅${i}场鸡公煲的爱情"))
    }

    val listState = rememberLazyListState()
    var markedSet: MutableSet<Int> = mutableSetOf()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    SnackbarUtils.initialize(snackbarHostState, coroutineScope)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBarView(scrollBehavior, listState, markedSet)
        },
        floatingActionButton = { AddItemFAB(cardItems, bitmapList6) },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }) { innerPadding ->
        MainView(innerPadding, listState, cardItems, markedSet)
    }
}
