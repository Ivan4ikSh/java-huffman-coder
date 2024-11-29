import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class Node {
    char ch;
    int freq;
    Node left = null;
    Node right = null;

    Node(char ch, int freq) {
        this.ch = ch;
        this.freq = freq;
    }

    Node(char ch, int freq, Node left, Node right) {
        this.ch = ch;
        this.freq = freq;
        this.left = left;
        this.right = right;
    }
}

class Huffman {
    private static final Map<Character, String> huffman_сode_map = new HashMap<>();

    public static void Encode(String text, String output_file_name) throws IOException {
        Node root = GenerateTree(text);
        EncodeNode(root, "", huffman_сode_map);

        StringBuilder encoded_text = new StringBuilder();
        for (char c : text.toCharArray()) {
            encoded_text.append(huffman_сode_map.get(c));
        }

        WriteEncodedDataToFile(output_file_name, encoded_text.toString());
    }

    public static void Decode(String input_file_name, String output_file_name) throws IOException {
        try (FileInputStream file_in = new FileInputStream(input_file_name); FileOutputStream file_out = new FileOutputStream(output_file_name)) {
            ReadCodeMapFromFile(file_in);
            String decoded_text = DecodeText(ReadEncodedTextFromFile(file_in));
            file_out.write(decoded_text.getBytes());
        }
    }

    private static void ReadCodeMapFromFile(FileInputStream file_in) throws IOException {
        int number_of_entries = file_in.read();
        for (int i = 0; i < number_of_entries; ++i) {
            char symbol = (char) file_in.read();
            int code_length = file_in.read();
            StringBuilder code = new StringBuilder();
            int bits_read = 0, buffer = 0;

            while (bits_read < code_length) {
                if (bits_read % 8 == 0) {
                    buffer = file_in.read();
                    if (buffer == -1) break;
                }
                int bit_pos = 7 - (bits_read % 8);
                code.append((buffer & (1 << bit_pos)) != 0 ? '1' : '0');
                bits_read++;
            }
            huffman_сode_map.put(symbol, code.toString());
        }
    }

    private static String ReadEncodedTextFromFile(FileInputStream file_in) throws IOException {
        StringBuilder encoded_text = new StringBuilder();
        int byte_read, byte_count = 0, text_size = file_in.read();

        while ((byte_read = file_in.read()) != -1) {
            for (int b = 7; b >= 0 && byte_count < text_size; --b) {
                encoded_text.append((byte_read & (1 << b)) != 0 ? '1' : '0');
                byte_count++;
                if (byte_count == text_size) break;
            }
        }

        return encoded_text.toString();
    }

    private static String DecodeText(String encoded_text) {
        Map<String, Character> reverse_code_map = new HashMap<>();

        for (Map.Entry<Character, String> entry : huffman_сode_map.entrySet()) {
            reverse_code_map.put(entry.getValue(), entry.getKey());
        }

        StringBuilder decoded_text = new StringBuilder();
        StringBuilder current_code = new StringBuilder();

        for (char bit : encoded_text.toCharArray()) {
            current_code.append(bit);
            if (reverse_code_map.containsKey(current_code.toString())) {
                decoded_text.append(reverse_code_map.get(current_code.toString()));
                current_code.setLength(0);
            }
        }

        return decoded_text.toString();
    }

    private static void WriteEncodedDataToFile(String filename, String encoded_text) throws IOException {
        try (FileOutputStream file_out = new FileOutputStream(filename)) {
            WriteCodeMapToFile(file_out);
            WriteEncodedTextToFile(file_out, encoded_text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void WriteEncodedTextToFile(FileOutputStream file_out, String encoded_text) throws IOException {
        file_out.write(encoded_text.length());
        int buffer = 0, bit_count = 0;

        for (char bit : encoded_text.toCharArray()) {
            if (bit == '1') buffer |= (1 << (7 - bit_count));
            bit_count++;

            if (bit_count == 8) {
                file_out.write(buffer);
                buffer = 0;
                bit_count = 0;
            }
        }

        if (bit_count > 0) { // Write remaining bits
            file_out.write(buffer);
        }
    }

    private static void WriteCodeMapToFile(FileOutputStream file_out) throws IOException {
        file_out.write(huffman_сode_map.size());

        for (Map.Entry<Character, String> entry : huffman_сode_map.entrySet()) {
            char symbol = entry.getKey();
            String code = entry.getValue();

            file_out.write(symbol);
            file_out.write(code.length());

            int buffer = 0, bit_count = 0;

            for (char bit : code.toCharArray()) {
                if (bit == '1') buffer |= (1 << (7 - bit_count));
                bit_count++;

                if (bit_count == 8) {
                    file_out.write(buffer);
                    buffer = 0;
                    bit_count = 0;
                }
            }

            if (bit_count > 0) { // Write remaining bits
                file_out.write(buffer);
            }
        }
    }

    private static void EncodeNode(Node root, String str, Map<Character, String> huffman_code) {
        if (root == null) return;

        if (root.left == null && root.right == null) {
            if (str.equals("")) huffman_code.put(root.ch, "0");
            else huffman_code.put(root.ch, str);
        }

        EncodeNode(root.left, str + "0", huffman_code);
        EncodeNode(root.right, str + "1", huffman_code);
    }

    private static Node GenerateTree(String text) {
        Map<Character, Integer> freq_map = new HashMap<>();

        for (char c : text.toCharArray()) {
            freq_map.put(c, freq_map.getOrDefault(c, 0) + 1); // Count frequency
        }

        PriorityQueue<Node> pq = new PriorityQueue<>((l, r) -> l.freq - r.freq);

        for (Map.Entry<Character, Integer> entry : freq_map.entrySet()) {
            pq.add(new Node(entry.getKey(), entry.getValue()));
        }

        while (pq.size() > 1) { // Build tree
            Node left = pq.poll();
            Node right = pq.poll();
            pq.add(new Node('\0', left.freq + right.freq, left, right));
        }

        return pq.peek();
    }

    public static String ReadFile(String file_path) throws IOException {
        StringBuilder content_builder = new StringBuilder();

        try (BufferedReader buffer_reader = new BufferedReader(new FileReader(new File(file_path)))) {
            String line;

            while ((line = buffer_reader.readLine()) != null) {
                content_builder.append(line).append("\n");
            }

            return content_builder.toString().trim();
        }
    }

    public static void main(String[] args) {
        if (args.length != 3 || (!args[0].equals("e") && !args[0].equals("d"))) {
            System.out.println("Usage: <e|d> <InputFileName> <OutputFileName>");
            return;
        }

        String mode = args[0];
        String input_file_path = args[1];
        String output_file_path = args[2];

        try {
            if ("e".equals(mode)) {
                Encode(ReadFile(input_file_path), output_file_path);
            } else if ("d".equals(mode)) {
                Decode(input_file_path, output_file_path);
            } else {
                throw new IllegalArgumentException("Invalid mode selected. Use 'e' for encoding or 'd' for decoding.");
            }
        } catch (IOException e) {
            System.err.println("File input/output error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("Operation completed successfully!");
    }
}