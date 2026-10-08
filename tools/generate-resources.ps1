# Rebuild the fixed palette models. References installed BOP textures; never copies them.
$ErrorActionPreference='Stop'
$resourceRoot=Join-Path $PSScriptRoot '../src/main/resources/assets/skysterrainsmootherbop'
$materials=@'
[{"name":"spectral_moss","textures":{"particle":"blocks/end_stone","bottom":"blocks/end_stone","top":"biomesoplenty:blocks/spectral_moss_top","side":"biomesoplenty:blocks/spectral_moss_side"},"tinted":false,"snowSide":"minecraft:blocks/grass_side_snowed"},{"name":"overgrown_stone","textures":{"particle":"blocks/stone","bottom":"blocks/stone","top":"blocks/grass_top","side":"biomesoplenty:blocks/overgrown_stone_side","overlay":"biomesoplenty:blocks/overgrown_stone_side_overlay"},"tinted":true,"snowSide":"biomesoplenty:blocks/overgrown_stone_side_snowed"},{"name":"loamy_grass_block","textures":{"particle":"biomesoplenty:blocks/dirt_loamy","bottom":"biomesoplenty:blocks/dirt_loamy","top":"biomesoplenty:blocks/grass_top","side":"biomesoplenty:blocks/grass_loamy_side","overlay":"biomesoplenty:blocks/grass_side_overlay"},"tinted":true,"snowSide":"biomesoplenty:blocks/grass_loamy_side_snowed"},{"name":"sandy_grass_block","textures":{"particle":"biomesoplenty:blocks/dirt_sandy","bottom":"biomesoplenty:blocks/dirt_sandy","top":"biomesoplenty:blocks/grass_top","side":"biomesoplenty:blocks/grass_sandy_side","overlay":"biomesoplenty:blocks/grass_side_overlay"},"tinted":true,"snowSide":"biomesoplenty:blocks/grass_sandy_side_snowed"},{"name":"silty_grass_block","textures":{"particle":"biomesoplenty:blocks/dirt_silty","bottom":"biomesoplenty:blocks/dirt_silty","top":"biomesoplenty:blocks/grass_top","side":"biomesoplenty:blocks/grass_silty_side","overlay":"biomesoplenty:blocks/grass_side_overlay"},"tinted":true,"snowSide":"biomesoplenty:blocks/grass_silty_side_snowed"},{"name":"origin_grass_block","textures":{"particle":"blocks/dirt","bottom":"blocks/dirt","top":"biomesoplenty:blocks/grass_origin_top","side":"biomesoplenty:blocks/grass_origin_side"},"tinted":false,"snowSide":"biomesoplenty:blocks/grass_origin_side_snowed"},{"name":"overgrown_netherrack","textures":{"particle":"blocks/netherrack","bottom":"blocks/netherrack","top":"biomesoplenty:blocks/overgrown_netherrack_top","side":"biomesoplenty:blocks/overgrown_netherrack_side"},"tinted":false,"snowSide":"minecraft:blocks/grass_side_snowed"},{"name":"daisy_grass_block","textures":{"particle":"blocks/dirt","bottom":"blocks/dirt","top":"biomesoplenty:blocks/grass_daisy_top","side":"biomesoplenty:blocks/grass_daisy_side"},"tinted":false,"snowSide":"biomesoplenty:blocks/grass_daisy_side_snowed"},{"name":"loamy_dirt","textures":{"all":"biomesoplenty:blocks/dirt_loamy"},"tinted":false,"snowSide":"minecraft:blocks/grass_side_snowed"},{"name":"sandy_dirt","textures":{"all":"biomesoplenty:blocks/dirt_sandy"},"tinted":false,"snowSide":"minecraft:blocks/grass_side_snowed"},{"name":"silty_dirt","textures":{"all":"biomesoplenty:blocks/dirt_silty"},"tinted":false,"snowSide":"minecraft:blocks/grass_side_snowed"},{"name":"coarse_loamy_dirt","textures":{"all":"biomesoplenty:blocks/coarse_dirt_loamy"},"tinted":false,"snowSide":"minecraft:blocks/grass_side_snowed"},{"name":"coarse_sandy_dirt","textures":{"all":"biomesoplenty:blocks/coarse_dirt_sandy"},"tinted":false,"snowSide":"minecraft:blocks/grass_side_snowed"},{"name":"coarse_silty_dirt","textures":{"all":"biomesoplenty:blocks/coarse_dirt_silty"},"tinted":false,"snowSide":"minecraft:blocks/grass_side_snowed"}]
'@ | ConvertFrom-Json
function Write-Resource($relative,$value) {
    $target=Join-Path $resourceRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
    [IO.File]::WriteAllText($target,(ConvertTo-Json -InputObject $value -Depth 16 -Compress)+"`n",[Text.UTF8Encoding]::new($false))
}
function Texture-Id($id) { if($id.Contains(':')) { return $id }; return 'minecraft:'+$id }
foreach($shape in @('slab','step','corner')) {
    $states=if($shape-eq'slab'){2}else{8}
    for($orientation=0;$orientation-lt$states;$orientation++) {
        $direction=$orientation%4
        $top=if($shape-eq'slab'){$orientation-eq0}else{$orientation-lt4}
        $x1=0;$z1=0;$x2=16;$z2=16
        if($shape-eq'step'){switch($direction){0{$z2=8}1{$x1=8}2{$z1=8}3{$x2=8}}}
        if($shape-eq'corner'){$x1=if($direction-in@(1,2)){8}else{0};$z1=if($direction-ge2){8}else{0};$x2=$x1+8;$z2=$z1+8}
        $y1=if($top){8}else{0};$y2=$y1+8
        foreach($tint in @($false,$true)) {
            $faces=[ordered]@{down=@{uv=@($x1,$z1,$x2,$z2);texture='#bottom'};up=@{uv=@($x1,$z1,$x2,$z2);texture='#top'}}
            if($tint){$faces.up.tintindex=0}
            foreach($face in @('north','south','west','east')) {
                $u1=if($face-in@('north','south')){$x1}else{$z1}
                $u2=if($face-in@('north','south')){$x2}else{$z2}
                $faces[$face]=@{uv=@($u1,0,$u2,8);texture='#side'}
            }
            $elements=@(@{from=@($x1,$y1,$z1);to=@($x2,$y2,$z2);faces=$faces})
            if($tint) {
                $overlay=[ordered]@{}
                foreach($face in @('north','south','west','east')){$overlay[$face]=@{uv=$faces[$face].uv;texture='#overlay';tintindex=0}}
                $elements+=@{from=@($x1,$y1,$z1);to=@($x2,$y2,$z2);faces=$overlay}
            }
            $suffix=if($tint){'_grass'}else{''}
            Write-Resource "models/block/base_${shape}_${orientation}${suffix}.json" @{parent='minecraft:block/block';elements=$elements}
        }
    }
    foreach($grass in @($false,$true)) {
        $count=if($grass){8}else{6};$capacity=[int](16/$states)
        $group=if($grass){'grass'}else{'dirt'}
        for($start=0;$start-lt$count;$start+=$capacity) {
            $id="${group}_${shape}_"+([int]($start/$capacity)).ToString('00')
            $variants=[ordered]@{}
            for($slot=0;$slot-lt[Math]::Min($capacity,$count-$start);$slot++) {
                $material=$materials[$(if($grass){$start+$slot}else{8+$start+$slot})]
                for($orientation=0;$orientation-lt$states;$orientation++) {
                    $meta=$slot*$states+$orientation
                    foreach($snow in @($false,$true)) {
                        $snowName=$snow.ToString().ToLowerInvariant()
                        $model="${id}_${meta}_${snowName}"
                        $texture=[ordered]@{}
                        if($grass){foreach($key in $material.textures.PSObject.Properties.Name){$texture[$key]=Texture-Id $material.textures.$key}}
                        else {foreach($key in @('particle','top','side','bottom')){$texture[$key]=Texture-Id $material.textures.all}}
                        if($snow){$texture.top='minecraft:blocks/snow';$texture.side=Texture-Id $material.snowSide}
                        $suffix=if($grass-and$material.tinted-and!$snow){'_grass'}else{''}
                        Write-Resource "models/block/$model.json" @{parent="skysterrainsmootherbop:block/base_${shape}_${orientation}${suffix}";textures=$texture}
                        $variants["meta=$meta,snowy=$snowName"]=@{model="skysterrainsmootherbop:$model"}
                    }
                }
            }
            for($reserved=[Math]::Min($capacity,$count-$start)*$states;$reserved-lt16;$reserved++) {
                foreach($snowName in @('false','true')) {$variants["meta=$reserved,snowy=$snowName"]=@{model="skysterrainsmootherbop:${id}_0_${snowName}"}}
            }
            Write-Resource "blockstates/$id.json" @{variants=$variants}
        }
    }
}
Write-Output 'Generated 16 fixed soil blockstates and their models.'
