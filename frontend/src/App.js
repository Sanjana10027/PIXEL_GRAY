import React, { useState, useRef } from "react";
import api from "./api/axios";
import "./App.css";

const MatrixOverlay = ({ linearData, width, height, pos, zoom, title, isLocked }) => {
  if (!linearData || !pos) return <div className="matrix-viewer empty">HOVER OR CLICK {title}</div>;

  const radius = 2;
  // Note: Since only result zooms now, px/py calculation depends on which viewport we are inspecting.
  // We'll keep the logic consistent with the original coordinate space.
  const px = Math.floor(pos.x); 
  const py = Math.floor(pos.y);
  const grid = [];

  for (let dy = -radius; dy <= radius; dy++) {
    const row = [];
    for (let dx = -radius; dx <= radius; dx++) {
      const x = px + dx; const y = py + dy;
      if (x >= 0 && x < width && y >= 0 && y < height) {
        const val = linearData[y * width + x];
        if (typeof val === 'object' && val !== null) {
          row.push(val);
        } else if (typeof val === 'number') {
          row.push({
            r: (val >> 16) & 0xFF,
            g: (val >> 8) & 0xFF,
            b: val & 0xFF
          });
        } else {
          row.push(null);
        }
      } else {
        row.push(null);
      }
    }
    grid.push(row);
  }

  return (
    <div className={`matrix-viewer ${isLocked ? 'locked' : ''}`}>
      <div className="matrix-header">
        <span className="matrix-title">{title}</span>
        <span className="matrix-coords">[{px}, {py}] {isLocked && "🔒"}</span>
      </div>
      <div className="matrix-grid">
        {grid.map((row, i) => (
          <div key={i} className="matrix-row">
            {row.map((pixel, j) => (
              <div key={j} className="matrix-cell rgb-cell" style={{ 
                backgroundColor: pixel ? `rgb(${pixel.r},${pixel.g},${pixel.b})` : '#111',
                color: (pixel && (pixel.r + pixel.g + pixel.b)/3 > 128) ? '#000' : '#fff',
                border: (i===2 && j===2) ? '2px solid #6366f1' : '1px solid #333'
              }}>
                {pixel ? (
                  <div className="rgb-values">
                    <span>{pixel.r}</span>
                    <span>{pixel.g}</span>
                    <span>{pixel.b}</span>
                  </div>
                ) : '--'}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};

export default function App() {
  const [originalUrl, setOriginalUrl] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [originalBlob, setOriginalBlob] = useState(null);
  const [sourceLinear, setSourceLinear] = useState(null);
  const [resultLinear, setResultLinear] = useState(null);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [loading, setLoading] = useState(false);
  const [mousePos, setMousePos] = useState(null);
  const [lockedPos, setLockedPos] = useState(null);
  const [isSquare, setIsSquare] = useState(null);

  // Tool States
  const [brightness, setBrightness] = useState(0);
  const [contrast, setContrast] = useState(0);
  const [blurIntensity, setBlurIntensity] = useState(0);
  const [sharpenIntensity, setSharpenIntensity] = useState(0);
  const [rotateAngle, setRotateAngle] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [isGrayscaleMode, setIsGrayscaleMode] = useState(false);
  
  // Crop & Drag States
  const [cropData, setCropData] = useState({ x: 0, y: 0, w: 0, h: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [startPos, setStartPos] = useState({ x: 0, y: 0 });
  
  const fileInputRef = useRef(null);

  const generateSourceData = (img) => {
    const canvas = document.createElement('canvas');
    canvas.width = img.width;
    canvas.height = img.height;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(img, 0, 0);
    const imageData = ctx.getImageData(0, 0, img.width, img.height).data;
    const fullData = [];
    for (let i = 0; i < imageData.length; i += 4) {
      fullData.push({ r: imageData[i], g: imageData[i+1], b: imageData[i+2] });
    }
    setSourceLinear(fullData);
  };

  const handleUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setOriginalBlob(file);
    const url = URL.createObjectURL(file);
    setOriginalUrl(url);
    const img = new Image();
    img.onload = () => {
      setDimensions({ width: img.width, height: img.height });
      setCropData({ x: 0, y: 0, w: 0, h: 0 });
      generateSourceData(img);
      checkIfSquare(file);
    };
    img.src = url;
    setPreviewUrl(null);
    setResultLinear(null);
    setLockedPos(null);
    resetToolStates();
  };

  const checkIfSquare = async (file) => {
    const fd = new FormData();
    fd.append("image", file);
    try {
      const res = await api.post("/is-square", fd);
      setIsSquare(res.data);
    } catch (err) {
      console.error("Square check failed", err);
    }
  };

  const resetToolStates = () => {
    setBrightness(0);
    setContrast(0);
    setBlurIntensity(0);
    setSharpenIntensity(0);
    setRotateAngle(0);
    setZoom(1);
    setIsGrayscaleMode(false);
    setCropData({ x: 0, y: 0, w: 0, h: 0 });
  };

  const handleResetAll = () => {
    if (!originalBlob) return;
    resetToolStates();
    setPreviewUrl(null);
    setResultLinear(null);
    setLockedPos(null);
    // Refresh dimensions from original in case crop was applied
    const img = new Image();
    img.onload = () => setDimensions({ width: img.width, height: img.height });
    img.src = originalUrl;
  };

  const handleViewportClick = (e) => {
    if (isDragging && (cropData.w > 5 || cropData.h > 5)) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    if (lockedPos) {
        setLockedPos(null);
    } else {
        setLockedPos({ x, y });
    }
  };

  const handleMouseDown = (e) => {
    if (!originalUrl) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const x = Math.floor(e.clientX - rect.left);
    const y = Math.floor(e.clientY - rect.top);
    
    setStartPos({ x, y });
    setIsDragging(true);
  };

  const handleMouseMoveViewport = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const currentX = Math.floor(e.clientX - rect.left);
    const currentY = Math.floor(e.clientY - rect.top);
    
    setMousePos({ x: currentX, y: currentY });

    if (isDragging) {
      const left = Math.max(0, Math.min(startPos.x, currentX));
      const top = Math.max(0, Math.min(startPos.y, currentY));
      const width = Math.min(dimensions.width - left, Math.abs(currentX - startPos.x));
      const height = Math.min(dimensions.height - top, Math.abs(currentY - startPos.y));
      
      setCropData({ x: left, y: top, w: width, h: height });
    }
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const processImage = async (endpoint, params = {}) => {
    if (!originalBlob) return;
    
    // FIX 1 & 2: Check grayscale mode even on intensity 0. If grayscale is ON, we must call the server.
    if (!isGrayscaleMode && (endpoint === 'blur' || endpoint === 'sharpen') && params.intensity === 0) {
        setPreviewUrl(originalUrl);
        setResultLinear(sourceLinear);
        return;
    }

    if (endpoint === 'crop' && (params.w <= 0 || params.h <= 0)) {
        alert("Please select a crop area by dragging on the original image.");
        return;
    }

    setLoading(true);
    const fd = new FormData();
    fd.append("image", originalBlob); 
    fd.append("grayscale", isGrayscaleMode); 

    Object.entries(params).forEach(([key, val]) => fd.append(key, val));

    try {
      const res = await api.post(`${endpoint}`, fd);
      setPreviewUrl(`data:image/png;base64,${res.data.image}`);
      setResultLinear(res.data.linear); 
      if (endpoint === 'crop') {
        setDimensions({ width: parseInt(params.w), height: parseInt(params.h) });
      }
    } catch (err) {
      console.error(`Error during ${endpoint}:`, err);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleGrayscale = () => {
    const newMode = !isGrayscaleMode;
    setIsGrayscaleMode(newMode);
    
    // Auto-refresh the view to apply/remove grayscale immediately
    if (originalBlob) {
        processImage('brightness', { level: brightness }); 
    }
  };

  return (
    <div className="pixel-app">
      <header className="brand-header">
        <div className="logo">PIXEL<span>GREY</span>.v1</div>
        <div className="header-controls">
            {isSquare !== null && (
            <div className={`square-badge ${isSquare ? 'is-square' : 'not-square'}`}>
                {isSquare ? "■ SQUARE_MATCH" : "▭ ASYMMETRIC"}
            </div>
            )}
            <button 
                className={`btn-secondary ${isGrayscaleMode ? 'active' : ''}`}
                onClick={handleToggleGrayscale}
                style={{
                    backgroundColor: isGrayscaleMode ? 'var(--accent)' : 'transparent', 
                    color: isGrayscaleMode ? 'white' : 'var(--text-dim)',
                    border: '1px solid var(--accent)'
                }}
            >
                {isGrayscaleMode ? "MODE: GRAYSCALE" : "MODE: COLOR"}
            </button>
        </div>

        <div className="status-indicator" style={{color: loading ? 'orange' : '#10b981'}}>
            {loading ? "BUSY" : "SYSTEM_READY"}
        </div>
      </header>

      <nav className="toolbar">
        <div className="tool-unit">
          <span className="tool-label">Analysis</span>
          <div className="status-box">
             <div className="stat-row">
                <span>Aspect:</span>
                <span style={{color: isSquare ? '#10b981' : '#f87171'}}>
                    {isSquare ? "1:1 Perfect" : "Non-Square"}
                </span>
             </div>
             <button className="btn-secondary" onClick={() => checkIfSquare(originalBlob)}>Re-Validate</button>
          </div>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Source</span>
          <button onClick={() => fileInputRef.current.click()}>Upload</button>
          <input type="file" ref={fileInputRef} onChange={handleUpload} hidden />
        </div>

        <div className="tool-unit">
          <span className="tool-label">Adjustments</span>
          <div className="slider-group">
            <div className="slider-item">
               <span>Bri({brightness})</span>
               <input type="range" min="-100" max="100" value={brightness} 
                onChange={e => setBrightness(+e.target.value)} 
                onMouseUp={() => processImage('brightness', { level: brightness })} />
            </div>
            <div className="slider-item">
               <span>Con({contrast})</span>
               <input type="range" min="-100" max="100" value={contrast} 
                onChange={e => setContrast(+e.target.value)} 
                onMouseUp={() => processImage('contrast', { level: contrast })} />
            </div>
          </div>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Filters</span>
          <div className="slider-group">
            <div className="slider-item">
               <span>Blur({blurIntensity})</span>
               <input type="range" min="0" max="20" step="1" value={blurIntensity} 
                onChange={e => setBlurIntensity(+e.target.value)} 
                onMouseUp={() => processImage('blur', { intensity: blurIntensity })} />
            </div>
            <div className="slider-item">
               <span>Sharp({sharpenIntensity})</span>
               <input type="range" min="0" max="20" step="1" value={sharpenIntensity} 
                onChange={e => setSharpenIntensity(+e.target.value)} 
                onMouseUp={() => processImage('sharpen', { intensity: sharpenIntensity })} />
            </div>
          </div>
        </div>

        <div className="tool-unit">
          <span className="tool-label">Transform</span>
          <div className="input-group">
            <input type="number" className="number-input" value={rotateAngle} onChange={(e) => setRotateAngle(e.target.value)} placeholder="Deg" />
            <button className="btn-secondary" onClick={() => processImage('rotate', { angle: rotateAngle })}>Rotate</button>
          </div>
          <div className="btn-group" style={{marginTop: '5px'}}>
            <button className="btn-secondary" onClick={() => processImage('flip/horizontal')}>Flip H</button>
            <button className="btn-secondary" onClick={() => processImage('flip/vertical')}>Flip V</button>
          </div>
        </div>

        <div className="tool-unit">
          <span className="tool-label">SubMatrix</span>
          <div className="crop-stats" style={{fontSize: '11px', color: '#6366f1', fontWeight: 'bold', lineHeight: '1.4'}}>
            POS: {cropData.x}, {cropData.y} <br/>
            AREA: {cropData.w}x{cropData.h}
          </div>
          <button className="btn-primary" style={{ width: '100%', marginTop: '5px' }} 
                  onClick={() => processImage('crop', cropData)}>Apply Crop</button>
        </div>

        <div className="tool-unit system-unit">
          <span className="tool-label">Zoom Result ({zoom}x)</span>
          <div className="slider-item">
             <input type="range" min="0.5" max="4" step="0.1" value={zoom} onChange={e => setZoom(+e.target.value)} />
          </div>
          <div className="btn-group" style={{marginTop: '5px'}}>
            <button className="btn-secondary" onClick={() => setLockedPos(null)}>Unlock Matrix</button>
            <button className="btn-secondary" onClick={handleResetAll}>Reset All</button>
          </div>
        </div>
      </nav>

      <main className="workspace">
        <section className="panel">
          <MatrixOverlay linearData={sourceLinear} width={dimensions.width} height={dimensions.height} pos={lockedPos || mousePos} zoom={1} title="SOURCE" isLocked={!!lockedPos} />
          <MatrixOverlay linearData={resultLinear} width={dimensions.width} height={dimensions.height} pos={lockedPos || mousePos} zoom={1} title="RESULT" isLocked={!!lockedPos} />
          <div className="metadata-box">
            DIMENSIONS: {dimensions.width} x {dimensions.height} <br/>
            INSPECTOR: {lockedPos ? "LOCKED 🔒" : "DYNAMIC"} <br/>
            (Click image to toggle Lock)
          </div>
        </section>

        <section className="preview-panel">
          <div className="comparison-view">
            <div className="viewport-half">
              <span className="view-tag">ORIGINAL</span>
              <div className="viewport-container" 
                   onClick={handleViewportClick}
                   onMouseDown={handleMouseDown}
                   onMouseMove={handleMouseMoveViewport}
                   onMouseUp={handleMouseUp}
                   onMouseLeave={handleMouseUp}
                   style={{ 
                       position: 'relative', 
                       cursor: 'crosshair', 
                       userSelect: 'none',
                       alignItems: 'flex-start',
                       justifyContent: 'flex-start'
                   }}>
                {originalUrl && (
                  <>
                    <img 
                        src={originalUrl} 
                        draggable="false" 
                        className="preview-img" 
                        style={{ transform: `scale(1)`, transformOrigin: '0 0' }} 
                    />
                    {(cropData.w > 0 || cropData.h > 0) && (
                      <div className="crop-selector-visual" style={{
                        position: 'absolute',
                        border: '2px dashed #6366f1',
                        backgroundColor: 'rgba(99, 102, 241, 0.2)',
                        left: cropData.x,
                        top: cropData.y,
                        width: cropData.w,
                        height: cropData.h,
                        pointerEvents: 'none',
                        zIndex: 10
                      }} />
                    )}
                  </>
                )}
              </div>
            </div>

            <div className="viewport-half">
              <span className="view-tag">RESULT (ZOOMED)</span>
                <div className="viewport-container" style={{ overflow: 'auto', alignItems: 'flex-start', justifyContent: 'flex-start' }}>
                  {previewUrl && (
                    <img src={previewUrl} 
                        className="preview-img" 
                        style={{ 
                          transform: `scale(${zoom})`, 
                          transformOrigin: 'top left' 
                        }} 
                    />
                  )}
                </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}