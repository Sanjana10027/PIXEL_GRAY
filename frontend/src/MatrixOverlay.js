import React from "react";
import "./MatrixOverlay.css";

export default function MatrixOverlay({
  linearData,
  width,
  height,
  pos,
  title,
  isLocked,
}) {
  if (!linearData || !pos) return null;

  const size = 5;
  const half = Math.floor(size / 2);

  const getColor = (x, y) => {
    if (x < 0 || y < 0 || x >= width || y >= height)
      return null;

    const idx = y * width + x;
    const px = linearData[idx];

    if (!px) return null;

    if (typeof px === "number") {
      return {
        r: (px >> 16) & 255,
        g: (px >> 8) & 255,
        b: px & 255,
      };
    }

    return px;
  };

  return (
    <div className="matrix-overlay">
      <h4>
        {title} {isLocked && "🔒"}
      </h4>

      <div className="matrix-grid">
        {Array.from({ length: size }).map((_, row) => (
          <div className="matrix-row" key={row}>
            {Array.from({ length: size }).map((_, col) => {
              const x = pos.x + col - half;
              const y = pos.y + row - half;
              const c = getColor(x, y);

              const isCenter =
                x === pos.x && y === pos.y;

              return (
                <div
                  key={col}
                  className={`matrix-cell ${
                    isCenter ? "center" : ""
                  }`}
                  style={{
                    background: c
                      ? `rgb(${c.r},${c.g},${c.b})`
                      : "#333",
                    color:
                      c &&
                      c.r + c.g + c.b > 380
                        ? "#000"
                        : "#fff",
                  }}
                >
                  {c ? `${c.r},${c.g},${c.b}` : ""}
                </div>
              );
            })}
          </div>
        ))}
      </div>

      <div className="coords">
        x: {pos.x}, y: {pos.y}
      </div>
    </div>
  );
}
