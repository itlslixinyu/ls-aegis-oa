import test from 'node:test'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync, readFileSync } from 'node:fs'
import path from 'node:path'

test('native chooser origin, remaining capacity and image bytes obey the same boundaries', () => {
  const directory = path.resolve('.local/image-upload/jvm-policy')
  mkdirSync(directory, { recursive: true })
  const file = path.join(directory, 'ImagePolicyCheck.java')
  writeFileSync(file, `import com.lsaegis.eoms.web.ImageSelectionPolicy;
public class ImagePolicyCheck {
 static void check(boolean value) { if (!value) throw new AssertionError(); }
 public static void main(String[] args) {
  String entry="http://127.0.0.1:25684/customer/auth";
  check(ImageSelectionPolicy.sameOrigin("http://127.0.0.1:25684/customer/apply",entry));
  check(!ImageSelectionPolicy.sameOrigin("http://127.0.0.1:25685/customer/apply",entry));
  check(!ImageSelectionPolicy.sameOrigin("https://127.0.0.1:25684/customer/apply",entry));
  check(!ImageSelectionPolicy.sameOrigin("http://user@127.0.0.1:25684/",entry));
  check(!ImageSelectionPolicy.sameOrigin("http://127.0.0.1.example:25684/",entry));
  check(!ImageSelectionPolicy.sameOrigin("data:text/html,test",entry));
  check(!ImageSelectionPolicy.sameOrigin(null,entry));
  check(ImageSelectionPolicy.sameOrigin("https://example.com:443/a","https://example.com/b"));
  check(ImageSelectionPolicy.selectionLimit("2",true)==2);
  check(ImageSelectionPolicy.selectionLimit("0",true)==0);
  check(ImageSelectionPolicy.selectionLimit("99",true)==9);
  check(ImageSelectionPolicy.selectionLimit("null",true)==9);
  check(ImageSelectionPolicy.selectionLimit("6",false)==1);
  check(ImageSelectionPolicy.supportedHeader(new byte[]{(byte)255,(byte)216,(byte)255}));
  check(ImageSelectionPolicy.supportedHeader(new byte[]{(byte)137,80,78,71,13,10,26,10}));
  check(!ImageSelectionPolicy.supportedHeader(new byte[]{(byte)137,80,78,71}));
  check(!ImageSelectionPolicy.supportedHeader("not an image".getBytes()));
  check(!ImageSelectionPolicy.supportedHeader(null));
  System.out.println("18 native policy assertions PASS");
 }
}`)
  execFileSync('javac', ['-encoding','UTF-8','-d',directory,'android-demo/shared/src/main/java/com/lsaegis/eoms/web/ImageSelectionPolicy.java',file])
  assert.match(execFileSync('java',['-cp',directory,'ImagePolicyCheck'],{encoding:'utf8'}),/18 native policy assertions PASS/)
})

test('both apps expose only the capture cache through a non-exported provider and no broad permissions', () => {
  for (const role of ['customer','engineer']) {
    const manifest = readFileSync(`android-demo/${role}-app/src/main/AndroidManifest.xml`,'utf8')
    assert.doesNotMatch(manifest, /android.permission.(CAMERA|READ_MEDIA_IMAGES|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|MANAGE_EXTERNAL_STORAGE|RECORD_AUDIO|ACCESS_BACKGROUND_LOCATION)/)
    assert.match(manifest, /android:name="androidx.core.content.FileProvider"[\s\S]*?android:exported="false"/)
    assert.match(manifest, /android:grantUriPermissions="true"/)
    const paths=readFileSync(`android-demo/${role}-app/src/main/res/xml/upload_paths.xml`,'utf8')
    assert.match(paths,/path="upload-capture\/"/)
    assert.doesNotMatch(paths,/<root-path|<external-path|path="\."|path="\/"/)
  }
})
