# Network Simulator

A visual network topology editor with NS-2 simulation capabilities.

## Features

### Visual Network Editor
- **Pan & Zoom**: Infinite canvas with mouse wheel zoom and middle-button/Alt-drag panning
- **Node Creation**: Left-click on empty space to add nodes
- **Link Creation**: Shift+drag between nodes to create links
- **Node Movement**: Drag nodes to move them (snaps to grid)
- **Node Deletion**: Right-click on a node to delete it
- **Selection**: Ctrl+click to select nodes, Ctrl+drag for area selection
- **Copy/Paste/Cut**: Ctrl+C/V/X with link preservation
- **Undo/Redo**: Ctrl+Z/Y for all operations

### Network Simulation
- **Protocol Support**: TCP, UDP, TCP/Reno, TCP/Newreno, TCP/Vegas
- **Applications**: FTP, CBR (Constant Bit Rate), Telnet, Exponential traffic
- **Queue Types**: DropTail, RED, FQ, SFQ
- **Trace Generation**: Automatic .tr and .nam file generation
- **NS-3 API Integration**: Cloud-based simulation via https://api.ns3.azaken.com
- **Results Viewer**: 
  - Network Logs tab: View trace files and simulation output
  - NAM Animation tab: View NAM animation scripts

## Running the Application

### Windows (PowerShell)
```powershell
.\run.ps1
```

### Windows (Command Prompt)
```cmd
run.bat
```

### Linux/Mac/Git Bash
```bash
./run.sh
```

## Workflow

1. **Create Network Topology**
   - Add nodes by clicking on the canvas
   - Connect nodes with Shift+drag
   - Arrange nodes as needed

2. **Run Simulation**
   - Go to `Simulate → Run Simulation...`
   - Configure simulation parameters:
     - Transport protocol (TCP/UDP variants)
     - Queue type
     - Application type (FTP, CBR, etc.)
     - Bandwidth and delay
     - Packet size and data rate
   - Click "Run Simulation"

3. **View Results**
   - Network Logs tab shows trace files and simulation output
   - NAM Animation tab shows the NAM script (can be viewed with NS-2 NAM tool)
   - Export logs to file if needed

## How the API Works

The application uses the NS-3 simulation API at `https://api.ns3.azaken.com/simulate`:

1. **Generates NS-2 TCL script** with your network topology and protocol configuration
2. **Uploads TCL file** to the API server
3. **Server runs NS-2 simulation** and generates trace/NAM files
4. **Downloads results.zip** containing:
   - `.tr` files (network traces)
   - `.nam` files (NAM animations)
   - `.log` or `.out` files (simulation output)
5. **Displays results** in the integrated viewer

## Architecture

```
src/
├── Components/          # Network components
│   ├── Node.java       # Network node with ID management
│   ├── Link.java       # Network link between nodes
│   └── ...
├── UI/                  # User interface
│   ├── NetworkEditor.java           # Main window
│   ├── CanvasPanel.java            # Drawing canvas
│   ├── SimulationConfigDialog.java # Simulation parameters
│   ├── SimulationResultsWindow.java # Results display
│   ├── LogViewerPanel.java         # Trace logs viewer
│   └── NAMViewerPanel.java         # NAM animation viewer
└── Exporters/           # TCL generation and API
    ├── NS2TclGenerator.java # Enhanced TCL generator
    ├── NS3ApiClient.java    # API client
    └── ItmTclFrame.java     # Legacy TCL exporter
```

## Requirements

- Java 8 or higher
- Internet connection (for cloud simulation)
- NS-2 NAM tool (optional, for viewing NAM animations locally)

## Notes

- The simulation runs on a remote NS-2 server, no local NS-2 installation required
- Generated TCL scripts can be exported and run locally if you have NS-2 installed
- NAM files can be viewed with the NAM tool: `nam network.nam`
