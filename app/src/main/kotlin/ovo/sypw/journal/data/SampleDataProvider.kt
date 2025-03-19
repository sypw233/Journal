package ovo.sypw.journal.data

import ovo.sypw.journal.R
import ovo.sypw.journal.model.JournalData

/**
 * 示例数据提供者
 * 负责生成应用中使用的示例数据
 */
object SampleDataProvider {

    /**
     * 生成示例图片列表
     * @param count 图片数量
     * @return 图片列表
     */
    private fun generateBitmapList(count: Int): MutableList<Int> {
        val bitmapList = mutableListOf<Int>()
        for (i in R.raw.test_image1..R.raw.test_image1 + count) {
            bitmapList.add(i)
        }
        return bitmapList
    }

    /**
     * 生成示例日记数据
     * @param context 上下文
     * @return 日记数据列表
     */
    fun generateSampleData(): MutableList<JournalData> {
        val cardItems = mutableListOf<JournalData>()

        // 生成不同数量的图片列表

        val bitmapList1 = generateBitmapList(0)
        val bitmapList2 = generateBitmapList(1)
        val bitmapList3 = generateBitmapList(2)
        val bitmapList4 = generateBitmapList(3)
        val bitmapList5 = generateBitmapList(4)
        val bitmapList6 = generateBitmapList(5)

        cardItems.add(
            JournalData(
                id = 0,
                images = bitmapList1,
                text = "《恋爱猪脚饭》——工地与猪脚饭交织的浪漫邂逅！\n" + "\"当你以为人生已经烂尾时，命运的混凝土搅拌机正在偷偷运转！\"\n" + "破产老哥黄夏揣着最后的房租钱，逃进花都城中村的握手楼。本想和小茂等挂壁老哥一起吃猪脚饭躺平摆烂，却意外邂逅工地女神\"陈嘉怡\"，从而开启新的土木逆袭人生。\n" + "爽了，干土木的又爽了！即使在底层已经彻底有了的我们，也能通过奋斗拥有美好的明天！"
            )
        )
        cardItems.add(
            JournalData(
                id = cardItems.size,
                images = bitmapList2,
                text = "和魏志阳邂逅${cardItems.size}场鸡公煲的爱情"
            )
        )
        cardItems.add(
            JournalData(
                id = cardItems.size,
                images = bitmapList3,
                text = "和魏志阳邂逅${cardItems.size}场鸡公煲的爱情"
            )
        )
        cardItems.add(
            JournalData(
                id = cardItems.size,
                images = bitmapList4,
                text = "和魏志阳邂逅${cardItems.size}场鸡公煲的爱情"
            )
        )
        cardItems.add(
            JournalData(
                id = cardItems.size,
                images = bitmapList5,
                text = "和魏志阳邂逅${cardItems.size}场鸡公煲的爱情"
            )
        )
        cardItems.add(
            JournalData(
                id = cardItems.size,
                images = bitmapList6,
                text = "和魏志阳邂逅${cardItems.size}场鸡公煲的爱情"
            )
        )
        (0..10).forEach {
            cardItems.add(
                JournalData(
                    id = cardItems.size,
                    images = generateBitmapList(5),
                    text = "和魏志阳邂逅${cardItems.size}场鸡公煲的爱情"
                )
            )
        }


        // 添加示例数据
//        cardItems.add(
//            JournalData(
//                id = cardItems.size,
//                location = LocationData(
//                    name = "失忆喷雾",
//                    latitude = 24.630353,
//                    longitude = 118.094141
//                ),
//                date = Date(),
//                images = bitmapList1,
//                text = "《恋爱猪脚饭》——工地与猪脚饭交织的浪漫邂逅！\n" + "\"当你以为人生已经烂尾时，命运的混凝土搅拌机正在偷偷运转！\"\n" + "破产老哥黄夏揣着最后的房租钱，逃进花都城中村的握手楼。本想和小茂等挂壁老哥一起吃猪脚饭躺平摆烂，却意外邂逅工地女神\"陈嘉怡\"，从而开启新的土木逆袭人生。\n" + "爽了，干土木的又爽了！即使在底层已经彻底有了的我们，也能通过奋斗拥有美好的明天！"
//            )
//        )
//        cardItems.add(
//            JournalData(
//                id = cardItems.size,
//                location = LocationData(
//                    name = "土木老哥",
//                    latitude = 24.630353,
//                    longitude = 118.094141
//                ),
//                images = bitmapList2,
//                date = Date(),
//                text = "花都七月黏稠的空气里飘着沥青融化的焦味，黄夏攥着塑封袋里最后三张百元钞，在城中村的肠粉摊前数了第五遍。手机还在不停震动，" + "\"金城建筑公司债务清算群\"的红色数字跳成37，像块烧红的铁烙在掌心。"
//            )
//        )
//        cardItems.add(
//            JournalData(
//                id = cardItems.size,
//                location = LocationData(
//                    name = "土木老哥",
//                    latitude = 24.630353,
//                    longitude = 118.094141
//                ),
//                images = bitmapList3,
//                date = Date(),
//                text = "黄夏这辈子做过最贵的决定，是在猪脚饭店用最后300块买了个爱情事故。\n" + "\n" + "当辣椒油泼在陈嘉怡工装裤上的瞬间，他精准计算出这个月要洗多少件文化衫才能抵干洗费。但当他抬头看到对方用电子测距仪测量油渍扩散半径时，突然意识到自己可能误入了某个高科技碰瓷团伙。\n" + "\n" + "\"根据GB/T 3920纺织品色牢度测试标准...\"陈嘉怡推了推银边护目镜，胸前的一级注册结构工程师徽章在霓虹灯下泛着冷光，\"建议你用60℃水温配合过氧化氢溶液...\"\n" + "\n" + "\"停！\"黄夏举起沾着卤汁的筷子投降，\"我赔你整条裤子，只要别让我回忆高考化学。\"\n" + "\n" + "蹲在隔壁扒饭的小茂突然喷出半颗卤蛋：\"陈工可是剑桥回来的混凝土西施，她那套工装顶你半年房租！\"\n" + "\n" + "整个大排档突然响起此起彼伏的咳嗽声，工人们默契地用筷子敲打餐盘，仿佛在庆祝第38个掉进女神陷阱的冤大头。黄夏盯着顺着桌腿蜿蜒的辣椒油，终于明白为什么这里的瓷砖缝都泛着可疑的红色——这分明是处决破产舔狗的行刑场！"
//            )
//        )
//        cardItems.add(
//            JournalData(
//                id = cardItems.size,
//                images = bitmapList4,
//                location = LocationData(
//                    name = "土木老哥",
//                    latitude = 24.630353,
//                    longitude = 118.094141
//                ),
//                date = Date(),
//                text = "暴雨倾盆的午夜，黄夏被警报声惊醒时，正梦见自己变成会说话的混凝土试块。工地探照灯穿透活动板房裂缝，在他脸上切出明暗交织的焦虑光谱。\n" + "\n" + "\"陈工被困在基坑了！\"安全员苏倩的安全帽上水钻乱颤，\"说是支撑架位移超标...\"\n" + "\n" + "黄夏抄起反光背心往外冲，突然被小茂拽住裤腰：\"你个搬砖临时工凑什么热闹？\"\n" + "\n" + "\"我见过那个魔鬼螺栓！\"黄夏眼前闪过三天前的月光，陈嘉怡教他认脚手架型号时，有个银亮螺栓在暗处诡异地反光，\"东南角三号架第十层，就像你喝完假酒乱转的眼珠子！\"\n" + "\n" + "当他连滚带爬冲进塌方区时，正撞见陈嘉怡在泥浆里画受力分析图。女工程师的白色安全帽已经变成奥利奥味，手里的激光笔却稳得像手术刀：\"现在需要重新计算被动区土压力...\"\n" + "\n" + "\"螺栓松了！\"黄夏扑过去抓住她手腕，突然发现对方睫毛上凝着泥水结晶，\"礼拜二晚上十点十七分，三号架第十层西南侧的高强螺栓，扭矩绝对不够！\"\n" + "\n" + "陈嘉怡的瞳孔在暴雨中收缩成猫科动物的竖线：\"你为什么会注意到...\"\n" + "\n" + "\"因为那天你站在月光下...\"黄夏的喉结可疑地滑动，\"像根会发光的钢筋混凝土！\""
//            )
//        )
//        cardItems.add(
//            JournalData(
//                id = cardItems.size,
//                images = bitmapList5,
//                text = "和魏志阳邂逅一场潮汕牛肉的爱情"
//            )
//        )
//        cardItems.add(
//            JournalData(
//                id = cardItems.size,
//                images = bitmapList6,
//                text = "和魏志阳邂逅999场潮汕牛肉的爱情"
//            )
//        )


        return cardItems
    }
}