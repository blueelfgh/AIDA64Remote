import XCTest
@testable import AIDA64Remote

final class SensorParserTests: XCTestCase {
    func testParseRealSseSample() {
        let line =
            "data: Page0|{|}SIV3|4099{|}Bar3p|100|#C0C0C0,#808080|#FFFFFF,#AAAAAA{|}SIV4|14{|}Bar4p|14|#C0C0C0,#808080|#FFFFFF,#AAAAAA{|}SIV7|350{|}SIV11|98{|}Bar11p|98|#C0C0C0,#808080|#FFFFFF,#AAAAAA{|}SIV12|56{|}Simple18|9898 MB{|}Simple19|39%{|}Simple20|DDR4-2400{|}Simple26|温度: 40°C{|}Simple29|N/A°{|}Simple31|2026-08-10{|}SIV22|0{|}Gph23p|0{|}"
        let sensors = SensorParser.parse(line)
        let map = SensorParser.toMap(sensors)

        XCTAssertEqual(map["SIV3"], "4099")
        XCTAssertEqual(map["SIV4"], "14")
        XCTAssertEqual(map["SIV11"], "98")
        XCTAssertEqual(map["Simple20"], "DDR4-2400")
        XCTAssertFalse(sensors.contains { $0.id.hasPrefix("Page") })
    }

    func testDashboardMappingMatchesLiveAida64Page() {
        let values = [
            "SIV3": "4099",
            "Bar3p": "100",
            "SIV4": "14",
            "Bar4p": "14",
            "SIV5": "",
            "SIV6": "",
            "SIV7": "350",
            "Bar7p": "6",
            "SIV9": "",
            "SIV10": "N/A",
            "Bar10p": "0",
            "SIV11": "98",
            "Bar11p": "98",
            "SIV12": "56",
            "Bar12p": "56",
            "SIV13": "",
            "Simple17": "6374 MB",
            "Simple18": "9898 MB",
            "Simple19": "39%",
            "Simple20": "DDR4-2400",
            "Simple26": "温度: 40°C",
            "Simple28": "68°",
            "Simple29": "N/A°",
            "Simple30": "40°",
            "Simple31": "2026-08-10",
            "Simple32": "7d 04:58",
            "SIV22": "0",
            "SIV33": "2.4",
            "SIV34": "2.9",
            "SIV35": "2280",
            "SIV36": "0",
        ]
        let dashboard = values.toDashboard(
            labels: ["Label2": "Intel Core i7-7700"],
            fpsHistory: [0],
            gpuHistory: []
        )

        XCTAssertEqual(dashboard.cpuName, "Intel Core i7-7700")
        XCTAssertEqual(dashboard.cpuClock, "4099")
        XCTAssertEqual(dashboard.cpuUsage, "14")
        XCTAssertEqual(dashboard.cpuClockBar, 1, accuracy: 0.001)
        XCTAssertEqual(dashboard.cpuUsageBar, 0.14, accuracy: 0.001)
        XCTAssertEqual(dashboard.gpuClock, "350")
        XCTAssertEqual(dashboard.vramUsed, "—")
        XCTAssertEqual(dashboard.vramFree, "—")
        XCTAssertEqual(dashboard.gpuTemp, "N/A°")
        XCTAssertEqual(dashboard.drives.map(\.letter), ["C", "D"])
        XCTAssertEqual(dashboard.drives[0].usage, "98")
        XCTAssertEqual(dashboard.drives[0].temp, "40°C")
        XCTAssertEqual(dashboard.drives[1].usage, "56")
        XCTAssertFalse(dashboard.drives.contains { $0.letter == "E" })
        XCTAssertEqual(dashboard.ramType, "DDR4-2400")
        XCTAssertEqual(dashboard.ramUsed, "9898 MB")
        XCTAssertEqual(dashboard.ramFree, "6374 MB")
        XCTAssertEqual(dashboard.ramUsage, "39%")
        XCTAssertEqual(dashboard.boardTemp, "40°")
        XCTAssertEqual(dashboard.date, "2026-08-10")
        XCTAssertEqual(dashboard.time, "7d 04:58")
        XCTAssertEqual(dashboard.fps, "0")
        XCTAssertEqual(dashboard.upload, "2.4")
        XCTAssertEqual(dashboard.cpuFan, "2280")
    }
}
