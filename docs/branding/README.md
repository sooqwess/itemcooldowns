# ItemCooldowns — approved brand assets

The owner selected the AI-generated hourglass icon and the matching AI-generated
banner. The banner was generated using that icon as its visual reference.
Final finishing is deliberately restrained: local highlight/color adjustment,
light output sharpening and size-specific downsampling. Geometry and lettering
were not redrawn. Brand illustrations are not gameplay screenshots.

- `banner.png`: approved 2048×512 PNG for the README and Modrinth description.
- `itemcooldowns-icon-512.png`: approved project icon; also copied to `/icon.png`.
- `itemcooldowns-icon-1024.png`: higher-resolution export.
- `originals/`: unchanged owner-selected AI raster originals.
- `polish_assets.py`: reproducible finishing/export script; requires Pillow.

Run `python3 polish_assets.py` here to generate all sizes and a before/after
preview. The 2048×512 banner export is named
`itemcooldowns-banner-2048x512.png`; copy it to `banner.png` to update the header.
The script cannot regenerate the AI source images from scratch.

Original client screenshots are kept separately in `../gallery/originals/`.
