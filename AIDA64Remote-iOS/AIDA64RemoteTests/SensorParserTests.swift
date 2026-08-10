import XCTest
@testable import AIDA64Remote

final class SensorParserTests: XCTestCase {
    func testParseRealSseSample() {
        let line =
            "data: Page0|{|}Simple3|49°{|}SIV4|5487{|}Bar4p|100|#C0C0C0,#808080|#FFFFFF,#AAAAAA{|}SIV5|10{|}Gph24p|0|{|}Simple26|温度: 30°C{|}"
        let sensors = SensorParser.parse(line)
        let map = SensorParser.toMap(sensors)

        XCTAssertEqual(map["Simple3"], "49°")
        XCTAssertEqual(map["SIV4"], "5487")
        XCTAssertEqual(map["Bar4p"], "100")
        XCTAssertEqual(map["SIV5"], "10")
        XCTAssertEqual(map["Gph24p"], "0")
        XCTAssertEqual(map["Simple26"], "温度: 30°C")
        XCTAssertFalse(sensors.contains { $0.id.hasPrefix("Page") })
        XCTAssertEqual(
            sensors.first { $0.id == "Simple3" },
            SensorItem(id: "Simple3", value: "49°")
        )
    }

    func testDashboardMappingUsesSensorIds() {
        let values = [
            "Simple3": "49°",
            "SIV4": "5487",
            "Bar4p": "100",
            "SIV5": "10",
            "Bar5p": "10",
            "Simple26": "温度: 30°C",
            "SIV22": "50",
        ]
        let dashboard = values.toDashboard(
            labels: ["Label2": "Test CPU"],
            fpsHistory: [1, 2, 3],
            gpuHistory: [4, 5]
        )
        XCTAssertEqual(dashboard.cpuName, "Test CPU")
        XCTAssertEqual(dashboard.cpuTemp, "49°")
        XCTAssertEqual(dashboard.cpuClock, "5487")
        XCTAssertEqual(dashboard.cpuClockBar, 1, accuracy: 0.001)
        XCTAssertEqual(dashboard.driveCTemp, "30°C")
        XCTAssertEqual(dashboard.volumeBar, 0.5, accuracy: 0.001)
        XCTAssertEqual(dashboard.fpsHistory, [1, 2, 3])
    }
}
