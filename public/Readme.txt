Source:
https://commons.wikimedia.org/wiki/File:Lunar_libration_with_phase_Oct_2007.gif
License:
Public domain

brew install potrace
cd public
# magick Lunar_libration_with_phase_Oct_2007.gif -define webp:lossless=true Lunar_libration_with_phase_Oct_2007.webp
mkdir tmp favicon tmp/png tmp/48x48
magick Lunar_libration_with_phase_Oct_2007.gif tmp/png/rename.png
cd tmp/png

mv rename-0.png 0.1.png; mv rename-1.png 0.png; mv rename-2.png 0.4.png; mv rename-3.png 1.png; mv rename-4.png 2.png; mv rename-5.png 4.png; mv rename-6.png 6.png; mv rename-7.png 8.png; mv rename-8.png 11.png; mv rename-9.png 14.png; mv rename-10.png 18.png; mv rename-11.png 22.png; mv rename-12.png 26.png; mv rename-13.png 31.png; mv rename-14.png 35.png; mv rename-15.png 40.png; mv rename-16.png 45.png; mv rename-17.png 51.png; mv rename-18.png 56.png; mv rename-19.png 61.png; mv rename-20.png 67.png; mv rename-21.png 72.png; mv rename-22.png 77.png; mv rename-23.png 82.png; mv rename-24.png 86.png; mv rename-25.png 90.png; mv rename-26.png 93.png; mv rename-27.png 96.png; mv rename-28.png 98.png; mv rename-29.png 99.png; mv rename-30.png 100.png; mv rename-31.png 99.9.png; mv rename-32.png 101.png; mv rename-33.png 102.png; mv rename-34.png 104.png; mv rename-35.png 107.png; mv rename-36.png 110.png; mv rename-37.png 114.png; mv rename-38.png 118.png; mv rename-39.png 123.png; mv rename-40.png 128.png; mv rename-41.png 133.png; mv rename-42.png 139.png; mv rename-43.png 144.png; mv rename-44.png 150.png; mv rename-45.png 155.png; mv rename-46.png 161.png; mv rename-47.png 166.png; mv rename-48.png 171.png; mv rename-49.png 175.png; mv rename-50.png 179.png; mv rename-51.png 183.png; mv rename-52.png 187.png; mv rename-53.png 190.png; mv rename-54.png 192.png; mv rename-55.png 195.png; mv rename-56.png 197.png; mv rename-57.png 198.png; mv rename-58.png 199.png

rm *.*.png
cd ../../favicon
mkdir 0 1 2 4 6 8 11 14 18 22 26 31 35 40 45 51 56 61 67 72 77 82 86 90 93 96 98 99 100 101 102 104 107 110 114 118 123 128 133 139 144 150 155 161 166 171 175 179 183 187 190 192 195 197 198 199
cd ..

magick tmp/png/*.png -crop +0+15 -trim -resize '480x480!' -bordercolor black -border 16 -modulate 130,100 -set filename:fn '%[basename]' 'favicon/%[filename:fn]/android-chrome-512x512.png'
magick tmp/png/*.png -crop +0+15 -trim -resize '480x480!' -bordercolor black -border 16 -modulate 130,100 -resize 192x192 -set filename:fn '%[basename]' 'favicon/%[filename:fn]/android-chrome-192x192.png'
magick tmp/png/*.png -crop +0+15 -trim -resize '480x480!' -bordercolor black -border 16 -modulate 130,100 -resize 180x180 -set filename:fn '%[basename]' 'favicon/%[filename:fn]/apple-touch-icon.png'
magick tmp/png/*.png -crop +0+15 -trim -resize '480x480!' -bordercolor black -border 16 -modulate 130,100 -resize 16x16 -set filename:fn '%[basename]' 'favicon/%[filename:fn]/favicon-16x16.png'
magick tmp/png/*.png -crop +0+15 -trim -resize '480x480!' -bordercolor black -border 16 -modulate 130,100 -resize 32x32 -set filename:fn '%[basename]' 'favicon/%[filename:fn]/favicon-32x32.png'
# Yes, 150x150 is recommended to be 270x270
magick tmp/png/*.png -crop +0+15 -trim -resize '480x480!' -bordercolor black -border 16 -modulate 130,100 -resize 270x270 -set filename:fn '%[basename]' 'favicon/%[filename:fn]/mstile-150x150.png'
magick tmp/png/*.png -crop +0+15 -trim -resize '480x480!' -bordercolor black -border 16 -modulate 130,100 -resize 48x48 -set filename:fn '%[basename]' 'tmp/48x48/%[filename:fn].png'
for i in 0 1 2 4 6 8 11 14 18 22 26 31 35 40 45 51 56 61 67 72 77 82 86 90 93 96 98 99 100 101 102 104 107 110 114 118 123 128 133 139 144 150 155 161 166 171 175 179 183 187 190 192 195 197 198 199; do magick tmp/48x48/$i.png favicon/$i/favicon-32x32.png favicon/$i/favicon-16x16.png favicon/$i/favicon.ico; done
for i in 0 1 2 4 6 8 11 14 18 22 26 31 35 40 45 51 56 61 67 72 77 82 86 90 93 96 98 99 100 101 102 104 107 110 114 118 123 128 133 139 144 150 155 161 166 171 175 179 183 187 190 192 195 197 198 199; do magick tmp/png/$i.png -crop +0+15 -trim -resize '480x480!' -brightness-contrast 39x100 -morphology Open Octagon -bordercolor black -border 16 favicon/$i/safari-pinned-tab.svg; done

rm -rf tmp
# delete, because they do not look nice
rm -rf favicon/0 favicon/1 favicon/2 favicon/4 favicon/197 favicon/198 favicon/199
