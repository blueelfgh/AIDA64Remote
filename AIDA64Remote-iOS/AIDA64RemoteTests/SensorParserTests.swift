import XCTest
@testable import AIDA64Remote

final class SensorParserTests: XCTestCase {
    func testParseRealSseSample() {
        let line =
            "data: Page0|{|}SIV4|4099{|}Bar4p|100|#C0C0C0,#808080|#FFFFFF,#AAAAAA{|}SIV5|14{|}Bar5p|14|#C0C0C0,#808080|#FFFFFF,#AAAAAA{|}SIV8|350{|}SIV12|98{|}Bar12p|98|#C0C0C0,#808080|#FFFFFF,#AAAAAA{|}SIV13|56{|}Simple18|9898 MB{|}Simple20|9898 MB{|}Simple21|DDR4-2400{|}Simple22|39%{|}Simple23|DDR4-2400{|}Simple26|温度: 40°C{|}Simple28|40°C{|}SIV11|N/A°{|}Simple33|2026-08-10{|}Simple34|7d 04:58{|}SIV25|0{|}SIV35|2.4{|}SIV36|2.9{|}SIV37|2280{|}SIV38|0{|}"
        let sensors = SensorParser.parse(line)
        let map = SensorParser.toMap(sensors)

        XCTAssertEqual(map["SIV4"], "4099")
        XCTAssertEqual(map["SIV5"], "14")
        XCTAssertEqual(map["SIV12"], "98")
        XCTAssertEqual(map["Simple23"], "DDR4-2400")
        XCTAssertFalse(sensors.contains { $0.id.hasPrefix("Page") })
    }

    /// 字段映射与 Android `Models.kt` 一致（家里 AIDA64 布局）。
    func testDashboardMappingMatchesAndroid() {
        let values = [
            "Simple3": "58°",
            "SIV4": "4099",
            "Bar4p": "100",
            "SIV5": "14",
            "Bar5p": "14",
            "SIV6": "2048",
            "Bar6p": "25",
            "SIV7": "6144",
            "Bar7p": "75",
            "SIV8": "350",
            "Bar8p": "6",
            "SIV9": "5001",
            "Bar9p": "50",
            "SIV10": "12",
            "Bar10p": "12",
            "SIV11": "45°",
            "Bar11p": "45",
            "SIV12": "98",
            "Bar12p": "98",
            "SIV13": "56",
            "Bar13p": "56",
            "SIV14": "30",
            "Bar14p": "30",
            "SIV18": "41°",
            "SIV19": "39°",
            "Simple20": "9898 MB",
            "Simple21": "6374 MB",
            "Simple22": "39%",
            "Simple23": "DDR4-2400",
            "Simple26": "温度: 40°C",
            "Simple28": "40°C",
            "Simple29": "38°C",
            "Simple30": "36°C",
            "Simple33": "2026-08-10",
            "Simple34": "7d 04:58",
            "SIV25": "0",
            "SIV35": "2.4",
            "SIV36": "2.9",
            "SIV37": "2280",
            "SIV38": "0",
            "Bar22p": "40",
        ]
        let dashboard = values.toDashboard(
            labels: ["Label2": "Intel Core i7-7700"],
            fpsHistory: [0],
            gpuHistory: []
        )

        XCTAssertEqual(dashboard.cpuName, "Intel Core i7-7700")
        XCTAssertEqual(dashboard.cpuTemp, "58°")
        XCTAssertEqual(dashboard.cpuClock, "4099")
        XCTAssertEqual(dashboard.cpuUsage, "14")
        XCTAssertEqual(dashboard.cpuClockBar, 1, accuracy: 0.001)
        XCTAssertEqual(dashboard.cpuUsageBar, 0.14, accuracy: 0.001)
        XCTAssertEqual(dashboard.gpuClock, "350")
        XCTAssertEqual(dashboard.vramUsed, "2048")
        XCTAssertEqual(dashboard.vramFree, "6144")
        XCTAssertEqual(dashboard.gpuTemp, "45°")
        XCTAssertEqual(dashboard.gpuUsage, "12")
        XCTAssertEqual(dashboard.drives.map(\.letter), ["C", "D", "E"])
        XCTAssertEqual(dashboard.drives[0].usage, "98")
        XCTAssertEqual(dashboard.drives[0].temp, "40°C")
        XCTAssertEqual(dashboard.drives[1].usage, "56")
        XCTAssertEqual(dashboard.drives[2].usage, "30")
        XCTAssertEqual(dashboard.ramType, "DDR4-2400")
        XCTAssertEqual(dashboard.ramUsed, "9898 MB")
        XCTAssertEqual(dashboard.ramFree, "6374 MB")
        XCTAssertEqual(dashboard.ramUsage, "39%")
        XCTAssertEqual(dashboard.ramTemp1, "41°")
        XCTAssertEqual(dashboard.ramTemp2, "39°")
        XCTAssertEqual(dashboard.date, "2026-08-10")
        XCTAssertEqual(dashboard.time, "7d 04:58")
        XCTAssertEqual(dashboard.fps, "0")
        XCTAssertEqual(dashboard.upload, "2.4")
        XCTAssertEqual(dashboard.download, "2.9")
        XCTAssertEqual(dashboard.cpuFan, "2280")
        XCTAssertEqual(dashboard.gpuFan, "0")
        XCTAssertEqual(dashboard.volumeBar, 0.4, accuracy: 0.001)
    }
}
