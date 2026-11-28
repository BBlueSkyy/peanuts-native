"""Synthetic tests for UE4.24 shared shader archives (no game data)."""
import struct, sys, tempfile, unittest, zlib
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'gfx' / 'tools'))
import uepak

class UEArchiveTest(unittest.TestCase):
    def test_v1_stored_and_zlib(self):
        a = b'LSLG' + b'A' * 200
        b = b'LSLG' + b'B' * 300
        c = zlib.compress(b)
        v1 = struct.pack('<II', 1, 2) + bytes(20) + struct.pack('<QIIB', 0, len(a), len(a), 0) + bytes(20) + struct.pack('<QIIB', len(a), len(c), len(b), 3) + a + c
        out = uepak.archive_v1_decoded(v1, 'fixture')
        table = uepak.archive(out)
        self.assertEqual(table['shaders'], [(0, len(a), len(a), 0), (len(a), len(b), len(b), 3)])
        self.assertEqual(out[table['code']:], a + b)
        with self.assertRaises(ValueError):
            uepak.archive_v1_decoded(v1[:-1], 'truncated')

    def test_v8_routing(self):
        with tempfile.TemporaryDirectory() as temp:
            path = str(Path(temp) / 'fixture.pak')
            Path(path).touch()
            with patch.object(uepak, 'footer', return_value=(0, 0, 0, [], 8)), patch.object(uepak, 'Pak') as pak, patch.object(uepak, 'inline_in_containers', return_value=False):
                for files, expected in [(['a.ushaderbytecode'], (False, True)), (['a.uexp'], (True, False)), (['a.ushaderbytecode', 'a.uexp'], (True, True))]:
                    pak.return_value.files = files
                    self.assertEqual(uepak.pak_shader_sources(path), expected)

if __name__ == '__main__':
    unittest.main()
